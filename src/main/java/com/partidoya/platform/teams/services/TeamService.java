package com.partidoya.platform.teams.services;

import com.partidoya.platform.auth.security.AuthenticatedUser;
import com.partidoya.platform.profiles.repositories.ProfilePersistenceEntity;
import com.partidoya.platform.profiles.repositories.ProfilePersistenceRepository;
import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.teams.dto.TeamResources;
import com.partidoya.platform.teams.models.*;
import com.partidoya.platform.teams.repositories.*;
import com.partidoya.platform.users.models.Role;
import com.partidoya.platform.users.repositories.UserPersistenceEntity;
import com.partidoya.platform.users.repositories.UserPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamService {
    private static final Logger log=LoggerFactory.getLogger(TeamService.class);
    private static final Set<String> SPORTS=Set.of("Fútbol","Básquet","Voleibol","Tenis");
    private static final Set<String> LEVELS=Set.of("BEGINNER","INTERMEDIATE","ADVANCED");
    private final TeamRepository teams; private final TeamMembershipRepository memberships;
    private final TeamRequestRepository requests; private final TeamChallengeRepository challenges;
    private final UserPersistenceRepository users; private final ProfilePersistenceRepository profiles;
    private final TeamImageStorage images;
    public TeamService(TeamRepository teams,TeamMembershipRepository memberships,TeamRequestRepository requests,
            TeamChallengeRepository challenges,UserPersistenceRepository users,ProfilePersistenceRepository profiles,TeamImageStorage images){
        this.teams=teams;this.memberships=memberships;this.requests=requests;this.challenges=challenges;this.users=users;this.profiles=profiles;this.images=images; }

    public TeamResources.Team create(AuthenticatedUser current, TeamResources.SaveTeam input){
        requirePlayer(current); validate(input); var normalized=normalize(input.name());
        if(teams.existsByNormalizedName(normalized)) throw new ResourceConflictException("Team","Ya existe un equipo con ese nombre");
        var team=new TeamEntity(); apply(team,input); team.setNormalizedName(normalized); team.setCaptainId(current.userId()); team.setStatus("ACTIVE"); team.setMaxMembers(30); team=teams.save(team);
        var member=new TeamMembershipEntity(); member.setTeamId(team.getId());member.setUserId(current.userId());member.setRole("CAPTAIN");member.setStatus("ACTIVE");member.setJoinedAt(LocalDateTime.now());memberships.save(member);
        return resource(team,current.userId());
    }

    @Transactional(readOnly=true)
    public TeamResources.TeamPage search(String search,String sport,String district,String level,Boolean open,int page,int size,Long viewer){
        var pageable=PageRequest.of(Math.max(0,page),Math.min(30,Math.max(1,size)),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));
        var result=teams.search(blank(search),blank(sport),blank(district),blank(level),open,pageable);
        var content=result.getContent();
        var ids=content.stream().map(TeamEntity::getId).toList();
        var memberRows=ids.isEmpty()?List.<TeamMembershipEntity>of():memberships.findByTeamIdInAndStatus(ids,"ACTIVE");
        var counts=memberRows.stream().collect(Collectors.groupingBy(TeamMembershipEntity::getTeamId,Collectors.counting()));
        var captainIds=content.stream().map(TeamEntity::getCaptainId).distinct().toList();
        var captainNames=users.findAllById(captainIds).stream().collect(Collectors.toMap(UserPersistenceEntity::getId,UserPersistenceEntity::getFullName));
        var viewerMemberships=viewer==null?Set.<Long>of():memberRows.stream().filter(m->viewer.equals(m.getUserId())).map(TeamMembershipEntity::getTeamId).collect(Collectors.toSet());
        var pending=viewer==null||ids.isEmpty()?Set.<Long>of():requests.findByTeamIdInAndUserIdAndTypeAndStatus(ids,viewer,"APPLICATION","PENDING").stream().map(TeamRequestEntity::getTeamId).collect(Collectors.toSet());
        var resources=content.stream().map(t->resource(t,viewer,captainNames.getOrDefault(t.getCaptainId(),"Capitán"),counts.getOrDefault(t.getId(),0L),viewerMemberships.contains(t.getId()),pending.contains(t.getId()))).toList();
        return new TeamResources.TeamPage(resources,result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
    }
    @Transactional(readOnly=true) public TeamResources.Team find(Long id,Long viewer){return resource(requireActive(id),viewer);}

    @Transactional(readOnly=true)
    public TeamResources.MyTeams mine(AuthenticatedUser current){ requirePlayer(current); var memberRows=memberships.findByUserIdAndStatus(current.userId(),"ACTIVE");
        var all=teams.findAllById(memberRows.stream().map(TeamMembershipEntity::getTeamId).toList()).stream().filter(t->"ACTIVE".equals(t.getStatus())).toList();
        var administered=all.stream().filter(t->t.getCaptainId().equals(current.userId())).map(t->resource(t,current.userId())).toList();
        var member=all.stream().filter(t->!t.getCaptainId().equals(current.userId())).map(t->resource(t,current.userId())).toList();
        var invitations=requests.findByUserIdAndTypeAndStatusOrderByCreatedAtDesc(current.userId(),"INVITATION","PENDING").stream().map(this::requestResource).toList();
        var applications=requests.findByUserIdAndTypeOrderByCreatedAtDesc(current.userId(),"APPLICATION").stream().map(this::requestResource).toList();
        var ids=all.stream().map(TeamEntity::getId).toList(); var teamChallenges=ids.isEmpty()?List.<TeamChallengeEntity>of():challenges.findForTeams(ids);
        long pending=invitations.size()+administered.stream().mapToLong(t->requests.countByTeamIdAndTypeAndStatus(t.id(),"APPLICATION","PENDING")).sum()+teamChallenges.stream().filter(c->"PENDING".equals(c.getStatus())&&ids.contains(c.getChallengedTeamId())).count();
        return new TeamResources.MyTeams(administered,member,invitations,applications,teamChallenges.stream().map(this::challengeResource).toList(),pending);
    }

    public TeamResources.Team update(AuthenticatedUser current,Long id,TeamResources.SaveTeam input){var team=requireCaptainLocked(current,id);validate(input);var normalized=normalize(input.name());
        if(!normalized.equals(team.getNormalizedName())&&teams.existsByNormalizedName(normalized))throw new ResourceConflictException("Team","Ya existe un equipo con ese nombre");
        apply(team,input);team.setNormalizedName(normalized);return resource(teams.save(team),current.userId());}
    public void deactivate(AuthenticatedUser current,Long id){
        var team=requireCaptainLocked(current,id);
        team.setStatus("INACTIVE");
        teams.save(team);
        requests.findByTeamIdAndStatusOrderByCreatedAt(id,"PENDING").forEach(r->resolve(r,"CANCELLED"));
        challenges.findForTeams(List.of(id)).stream().filter(c->"PENDING".equals(c.getStatus())).forEach(c->{
            c.setStatus("CANCELLED");
            c.setResolvedAt(LocalDateTime.now());
            challenges.save(c);
        });
    }

    public TeamResources.Logo uploadLogo(AuthenticatedUser current,Long id,MultipartFile file){
        var team=requireCaptainLocked(current,id);
        if(file==null||file.isEmpty()||file.getSize()>5_000_000)throw new IllegalArgumentException("El escudo debe ser una imagen de hasta 5 MB");
        var type=file.getContentType();
        if(type==null||!Set.of("image/png","image/jpeg","image/webp").contains(type))throw new IllegalArgumentException("Formato de imagen no permitido");
        String uploadedPublicId=null;
        try{
            var content=file.getBytes();
            validateImageContent(content);
            var uploaded=images.upload(content,file.getOriginalFilename(),id);
            uploadedPublicId=uploaded.publicId();
            var old=team.getLogoPublicId();
            team.setLogoPublicId(uploaded.publicId());
            team.setLogoUrl(uploaded.secureUrl());
            teams.save(team);
            teams.flush();
            try{images.delete(old);}catch(RuntimeException exception){log.warn("Could not remove previous logo for team {}",id,exception);}
            return new TeamResources.Logo(uploaded.secureUrl());
        } catch(java.io.IOException e){
            throw new IllegalStateException("No se pudo leer el escudo");
        } catch(RuntimeException exception){
            if(uploadedPublicId!=null){try{images.delete(uploadedPublicId);}catch(RuntimeException cleanup){log.warn("Could not clean uploaded logo for team {}",id,cleanup);}}
            throw exception;
        }
    }

    @Transactional(readOnly=true)
    public List<TeamResources.Member> members(Long id){requireActive(id);var rows=memberships.findByTeamIdAndStatusOrderByJoinedAt(id,"ACTIVE");var ids=rows.stream().map(TeamMembershipEntity::getUserId).toList();
        var userMap=users.findAllById(ids).stream().collect(Collectors.toMap(UserPersistenceEntity::getId,Function.identity()));var profileMap=profiles.findByUserIdIn(ids).stream().collect(Collectors.toMap(ProfilePersistenceEntity::getUserId,Function.identity()));
        return rows.stream().map(m->{var u=userMap.get(m.getUserId());var p=profileMap.get(m.getUserId());return new TeamResources.Member(m.getUserId(),u==null?"Jugador":u.getFullName(),p==null?null:p.getAvatarUrl(),m.getRole(),p==null?null:p.getPrimarySport(),p==null?null:p.getSkillLevel(),m.getJoinedAt());}).toList();}

    @Transactional(readOnly=true)
    public List<TeamResources.Player> searchPlayers(AuthenticatedUser current,Long teamId,String query){requireCaptain(current,teamId);var found=users.search(blank(query),Role.JUGADOR.name(),PageRequest.of(0,15));var ids=found.stream().map(UserPersistenceEntity::getId).toList();var ps=profiles.findByUserIdIn(ids).stream().collect(Collectors.toMap(ProfilePersistenceEntity::getUserId,Function.identity()));
        return found.stream().filter(u->memberships.findByTeamIdAndUserId(teamId,u.getId()).map(m->!"ACTIVE".equals(m.getStatus())).orElse(true)).map(u->{var p=ps.get(u.getId());return new TeamResources.Player(u.getId(),u.getFullName(),p==null?null:p.getAvatarUrl(),p==null?null:p.getSkillLevel());}).toList();}

    @Transactional(readOnly=true)
    public TeamResources.PublicPlayer publicPlayer(Long userId){
        var user=requirePlayerUser(userId);
        var profile=profiles.findByUserId(userId).orElse(null);
        return new TeamResources.PublicPlayer(userId,user.getFullName(),profile==null?null:profile.getAvatarUrl(),
                profile==null?null:profile.getDistrict(),profile==null?null:profile.getPrimarySport(),
                profile==null?null:profile.getSkillLevel());
    }

    public TeamResources.TeamRequest invite(AuthenticatedUser current,Long teamId,Long userId){requireCaptain(current,teamId);requirePlayerUser(userId);ensureCanJoin(teamId,userId);return requestResource(newRequest(teamId,userId,current.userId(),"INVITATION",LocalDateTime.now().plusDays(14)));}
    public TeamResources.TeamRequest apply(AuthenticatedUser current,Long teamId){requirePlayer(current);var team=requireActive(teamId);if(!team.isOpenToRequests())throw new ForbiddenActionException("Este equipo es privado");ensureCanJoin(teamId,current.userId());return requestResource(newRequest(teamId,current.userId(),current.userId(),"APPLICATION",null));}
    @Transactional(readOnly=true) public List<TeamResources.TeamRequest> pendingRequests(AuthenticatedUser current,Long teamId){requireCaptain(current,teamId);return requests.findByTeamIdAndTypeAndStatusOrderByCreatedAt(teamId,"APPLICATION","PENDING").stream().map(this::requestResource).toList();}
    public TeamResources.TeamRequest respondInvitation(AuthenticatedUser current,Long id,boolean accept){var r=requirePending(id,"INVITATION");if(current==null||!r.getUserId().equals(current.userId()))throw new ForbiddenActionException("La invitación no te pertenece");if(accept)addMember(r.getTeamId(),r.getUserId());resolve(r,accept?"ACCEPTED":"REJECTED");return requestResource(r);}
    public TeamResources.TeamRequest respondApplication(AuthenticatedUser current,Long id,boolean accept){var r=requirePending(id,"APPLICATION");requireCaptain(current,r.getTeamId());if(accept)addMember(r.getTeamId(),r.getUserId());resolve(r,accept?"APPROVED":"REJECTED");return requestResource(r);}

    public void removeMember(AuthenticatedUser current,Long teamId,Long userId){var team=requireCaptainLocked(current,teamId);if(team.getCaptainId().equals(userId))throw new ResourceConflictException("Team","El capitán no puede ser expulsado");endMembership(teamId,userId,"REMOVED");}
    public void leave(AuthenticatedUser current,Long teamId){requirePlayer(current);var team=requireActive(teamId);if(team.getCaptainId().equals(current.userId()))throw new ResourceConflictException("Team","Transfiere la capitanía antes de abandonar el equipo");endMembership(teamId,current.userId(),"LEFT");}
    public TeamResources.Team transfer(AuthenticatedUser current,Long teamId,Long newCaptain){var team=requireCaptainLocked(current,teamId);if(newCaptain==null||newCaptain.equals(current.userId()))throw new IllegalArgumentException("Selecciona otro integrante como capitán");var next=memberships.findByTeamIdAndUserId(teamId,newCaptain).filter(m->"ACTIVE".equals(m.getStatus())).orElseThrow(()->new ResourceConflictException("Team","El nuevo capitán debe ser integrante activo"));
        var old=memberships.findByTeamIdAndUserId(teamId,current.userId()).orElseThrow();old.setRole("MEMBER");next.setRole("CAPTAIN");memberships.save(old);memberships.save(next);team.setCaptainId(newCaptain);return resource(teams.save(team),current.userId());}

    public TeamResources.Challenge challenge(AuthenticatedUser current,Long challengedId,TeamResources.CreateChallenge input){requirePlayer(current);if(input==null||input.challengerTeamId()==null)throw new IllegalArgumentException("Selecciona el equipo desafiante");var source=requireCaptainLocked(current,input.challengerTeamId());var target=requireActive(challengedId);if(source.getId().equals(target.getId()))throw new IllegalArgumentException("Un equipo no puede desafiarse a sí mismo");if(!source.getSport().equals(target.getSport()))throw new ResourceConflictException("Challenge","Los equipos deben practicar el mismo deporte");
        if(challenges.findFirstByChallengerTeamIdAndChallengedTeamIdAndStatus(source.getId(),target.getId(),"PENDING").isPresent())throw new ResourceConflictException("Challenge","Ya existe un desafío pendiente");var c=new TeamChallengeEntity();c.setChallengerTeamId(source.getId());c.setChallengedTeamId(target.getId());c.setCreatedBy(current.userId());c.setSport(source.getSport());c.setMessage(trim(input.message(),300));c.setStatus("PENDING");return challengeResource(challenges.save(c));}
    public TeamResources.Challenge respondChallenge(AuthenticatedUser current,Long id,String action){var c=challenges.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Challenge",id.toString()));if(!"PENDING".equals(c.getStatus()))throw new ResourceConflictException("Challenge","El desafío ya fue resuelto");
        if("CANCELLED".equals(action)){requireCaptain(current,c.getChallengerTeamId());}else{requireCaptain(current,c.getChallengedTeamId());if(!Set.of("ACCEPTED","REJECTED").contains(action))throw new IllegalArgumentException("Acción de desafío inválida");}c.setStatus(action);c.setResolvedAt(LocalDateTime.now());return challengeResource(challenges.save(c));}

    private void addMember(Long teamId,Long userId){var team=teams.findLockedById(teamId).filter(t->"ACTIVE".equals(t.getStatus())).orElseThrow(()->new ResourceNotFoundException("Team",teamId.toString()));if(memberships.findByTeamIdAndUserId(teamId,userId).filter(m->"ACTIVE".equals(m.getStatus())).isPresent())throw new ResourceConflictException("Team","El jugador ya pertenece al equipo");if(memberships.countByTeamIdAndStatus(teamId,"ACTIVE")>=team.getMaxMembers())throw new ResourceConflictException("Team","El equipo alcanzó el límite de integrantes");var member=memberships.findByTeamIdAndUserId(teamId,userId).orElseGet(TeamMembershipEntity::new);member.setTeamId(teamId);member.setUserId(userId);member.setRole("MEMBER");member.setStatus("ACTIVE");member.setJoinedAt(LocalDateTime.now());member.setEndedAt(null);memberships.save(member);}
    private void ensureCanJoin(Long teamId,Long userId){var team=teams.findLockedById(teamId).filter(t->"ACTIVE".equals(t.getStatus())).orElseThrow(()->new ResourceNotFoundException("Team",teamId.toString()));if(memberships.findByTeamIdAndUserId(teamId,userId).filter(m->"ACTIVE".equals(m.getStatus())).isPresent())throw new ResourceConflictException("Team","El jugador ya pertenece al equipo");if(memberships.countByTeamIdAndStatus(teamId,"ACTIVE")>=team.getMaxMembers())throw new ResourceConflictException("Team","El equipo alcanzó el límite de integrantes");for(var type:List.of("INVITATION","APPLICATION"))if(requests.findFirstByTeamIdAndUserIdAndTypeAndStatus(teamId,userId,type,"PENDING").isPresent())throw new ResourceConflictException("Team","Ya existe una solicitud o invitación pendiente");}
    private TeamRequestEntity newRequest(Long teamId,Long userId,Long actor,String type,LocalDateTime expires){var r=new TeamRequestEntity();r.setTeamId(teamId);r.setUserId(userId);r.setRequestedBy(actor);r.setType(type);r.setStatus("PENDING");r.setExpiresAt(expires);return requests.save(r);}
    private TeamRequestEntity requirePending(Long id,String type){var r=requests.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Team request",id.toString()));if(!type.equals(r.getType()))throw new ResourceConflictException("Team request","La solicitud no corresponde a esta acción");if(!"PENDING".equals(r.getStatus()))throw new ResourceConflictException("Team request","La solicitud ya no está disponible");if(r.getExpiresAt()!=null&&r.getExpiresAt().isBefore(LocalDateTime.now())){resolve(r,"EXPIRED");throw new ResourceConflictException("Team request","La solicitud ya no está disponible");}return r;}
    private void resolve(TeamRequestEntity r,String status){r.setStatus(status);r.setResolvedAt(LocalDateTime.now());requests.save(r);}
    private void endMembership(Long teamId,Long userId,String status){var m=memberships.findByTeamIdAndUserId(teamId,userId).filter(x->"ACTIVE".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Team member",userId.toString()));m.setStatus(status);m.setEndedAt(LocalDateTime.now());memberships.save(m);}
    private TeamEntity requireCaptain(AuthenticatedUser current,Long id){requirePlayer(current);var t=requireActive(id);if(!t.getCaptainId().equals(current.userId()))throw new ForbiddenActionException("Solo el capitán puede administrar el equipo");return t;}
    private TeamEntity requireCaptainLocked(AuthenticatedUser current,Long id){requirePlayer(current);var t=teams.findLockedById(id).filter(team->"ACTIVE".equals(team.getStatus())).orElseThrow(()->new ResourceNotFoundException("Team",id.toString()));if(!t.getCaptainId().equals(current.userId()))throw new ForbiddenActionException("Solo el capitán puede administrar el equipo");return t;}
    private TeamEntity requireActive(Long id){return teams.findById(id).filter(t->"ACTIVE".equals(t.getStatus())).orElseThrow(()->new ResourceNotFoundException("Team",id.toString()));}
    private void requirePlayer(AuthenticatedUser current){if(current==null||current.role()!=Role.JUGADOR)throw new ForbiddenActionException("Esta acción requiere una cuenta de jugador");}
    private UserPersistenceEntity requirePlayerUser(Long id){return users.findById(id).filter(u->Role.JUGADOR.name().equals(u.getRole())&&"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ResourceNotFoundException("Player",id.toString()));}
    private void validate(TeamResources.SaveTeam i){if(i==null||i.name()==null||i.name().trim().length()<3||i.name().trim().length()>80)throw new IllegalArgumentException("El nombre debe tener entre 3 y 80 caracteres");if(!SPORTS.contains(i.sport()))throw new IllegalArgumentException("Deporte no admitido");if(i.description()==null||i.description().trim().length()<10||i.description().trim().length()>500)throw new IllegalArgumentException("La descripción debe tener entre 10 y 500 caracteres");if(!LEVELS.contains(i.competitiveLevel()))throw new IllegalArgumentException("Nivel competitivo inválido");if(i.district()!=null&&i.district().length()>80)throw new IllegalArgumentException("Distrito inválido");}
    private void apply(TeamEntity t,TeamResources.SaveTeam i){t.setName(i.name().trim().replaceAll("\\s+"," "));t.setSport(i.sport());t.setDescription(i.description().trim());t.setDistrict(blank(i.district()));t.setCompetitiveLevel(i.competitiveLevel());t.setOpenToRequests(i.openToRequests());}
    private TeamResources.Team resource(TeamEntity t,Long viewer){var captain=users.findById(t.getCaptainId()).map(UserPersistenceEntity::getFullName).orElse("Capitán");var membership=viewer==null?Optional.<TeamMembershipEntity>empty():memberships.findByTeamIdAndUserId(t.getId(),viewer).filter(m->"ACTIVE".equals(m.getStatus()));var pending=viewer!=null&&requests.findFirstByTeamIdAndUserIdAndTypeAndStatus(t.getId(),viewer,"APPLICATION","PENDING").isPresent();var member=membership.isPresent();var isCaptain=viewer!=null&&t.getCaptainId().equals(viewer);return new TeamResources.Team(t.getId(),t.getName(),t.getSport(),t.getDescription(),t.getLogoUrl(),t.getDistrict(),t.getCompetitiveLevel(),t.isOpenToRequests(),t.getStatus(),t.getCaptainId(),captain,memberships.countByTeamIdAndStatus(t.getId(),"ACTIVE"),t.getMaxMembers(),t.getCreatedAt(),member,isCaptain,pending,viewerRelationship(viewer,isCaptain,member,pending),new TeamResources.TeamStatistics(false,0,0,0,0,0));}
    private TeamResources.Team resource(TeamEntity t,Long viewer,String captain,long memberCount,boolean member,boolean pending){var isCaptain=viewer!=null&&t.getCaptainId().equals(viewer);return new TeamResources.Team(t.getId(),t.getName(),t.getSport(),t.getDescription(),t.getLogoUrl(),t.getDistrict(),t.getCompetitiveLevel(),t.isOpenToRequests(),t.getStatus(),t.getCaptainId(),captain,memberCount,t.getMaxMembers(),t.getCreatedAt(),member,isCaptain,pending,viewerRelationship(viewer,isCaptain,member,pending),new TeamResources.TeamStatistics(false,0,0,0,0,0));}
    private static String viewerRelationship(Long viewer,boolean captain,boolean member,boolean pending){if(viewer==null)return "ANONYMOUS";if(captain)return "CAPTAIN";if(member)return "MEMBER";if(pending)return "PENDING";return "EXTERNAL";}
    private TeamResources.TeamRequest requestResource(TeamRequestEntity r){var team=teams.findById(r.getTeamId()).orElse(null);var user=users.findById(r.getUserId()).orElse(null);return new TeamResources.TeamRequest(r.getId(),r.getTeamId(),team==null?"Equipo":team.getName(),r.getUserId(),user==null?"Jugador":user.getFullName(),r.getType(),r.getStatus(),r.getCreatedAt(),r.getExpiresAt());}
    private TeamResources.Challenge challengeResource(TeamChallengeEntity c){return new TeamResources.Challenge(c.getId(),c.getChallengerTeamId(),teams.findById(c.getChallengerTeamId()).map(TeamEntity::getName).orElse("Equipo"),c.getChallengedTeamId(),teams.findById(c.getChallengedTeamId()).map(TeamEntity::getName).orElse("Equipo"),c.getSport(),c.getMessage(),c.getStatus(),c.getMatchId(),c.getCreatedAt());}
    private static String normalize(String v){return Normalizer.normalize(v.trim().replaceAll("\\s+"," "),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
    private static String blank(String v){return v==null||v.isBlank()?null:v.trim();} private static String trim(String v,int max){v=blank(v);return v==null?null:v.substring(0,Math.min(max,v.length()));}
    private static void validateImageContent(byte[] bytes){var jpeg=bytes.length>2&&(bytes[0]&0xff)==0xff&&(bytes[1]&0xff)==0xd8;var png=bytes.length>8&&(bytes[0]&0xff)==0x89&&bytes[1]==0x50&&bytes[2]==0x4e&&bytes[3]==0x47;var webp=bytes.length>12&&bytes[0]=='R'&&bytes[1]=='I'&&bytes[2]=='F'&&bytes[3]=='F'&&bytes[8]=='W'&&bytes[9]=='E'&&bytes[10]=='B'&&bytes[11]=='P';if(!jpeg&&!png&&!webp)throw new IllegalArgumentException("Solo se permiten imágenes JPG, PNG o WebP válidas");}
}

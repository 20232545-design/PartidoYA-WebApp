package com.partidoya.platform.teams.controllers;

import com.partidoya.platform.auth.security.AuthenticatedUser;
import com.partidoya.platform.teams.dto.TeamResources;
import com.partidoya.platform.teams.services.TeamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Permanent team discovery, membership and challenge management")
public class TeamController {
    private final TeamService service;
    public TeamController(TeamService service){this.service=service;}

    @PostMapping public ResponseEntity<TeamResources.Team> create(@AuthenticationPrincipal AuthenticatedUser user,@RequestBody TeamResources.SaveTeam body){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user,body));}
    @GetMapping public TeamResources.TeamPage search(@RequestParam(required=false) String search,@RequestParam(required=false) String sport,@RequestParam(required=false) String district,@RequestParam(required=false) String level,@RequestParam(required=false) Boolean open,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,@AuthenticationPrincipal AuthenticatedUser user){return service.search(search,sport,district,level,open,page,size,user==null?null:user.userId());}
    @GetMapping("/{id}") public TeamResources.Team find(@PathVariable Long id,@AuthenticationPrincipal AuthenticatedUser user){return service.find(id,user==null?null:user.userId());}
    @GetMapping("/{id}/members") public List<TeamResources.Member> members(@PathVariable Long id){return service.members(id);}
    @GetMapping("/mine") public TeamResources.MyTeams mine(@AuthenticationPrincipal AuthenticatedUser user){return service.mine(user);}
    @PutMapping("/{id}") public TeamResources.Team update(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestBody TeamResources.SaveTeam body){return service.update(user,id,body);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){service.deactivate(user,id);return ResponseEntity.noContent().build();}
    @PostMapping(value="/{id}/logo",consumes="multipart/form-data") public TeamResources.Logo logo(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestPart("file") MultipartFile file){return service.uploadLogo(user,id,file);}
    @GetMapping("/{id}/players") public List<TeamResources.Player> players(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestParam(required=false) String search){return service.searchPlayers(user,id,search);}
    @GetMapping("/players/{userId}") public TeamResources.PublicPlayer publicPlayer(@PathVariable Long userId){return service.publicPlayer(userId);}
    @PostMapping("/{id}/invitations") public TeamResources.TeamRequest invite(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestBody TeamResources.InvitePlayer body){return service.invite(user,id,body.userId());}
    @PostMapping("/{id}/requests") public TeamResources.TeamRequest apply(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return service.apply(user,id);}
    @GetMapping("/{id}/requests") public List<TeamResources.TeamRequest> requests(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){return service.pendingRequests(user,id);}
    @PostMapping("/invitations/{id}/{action}") public TeamResources.TeamRequest invitation(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@PathVariable String action){if(!List.of("accept","reject").contains(action))throw new IllegalArgumentException("Acción de invitación inválida");return service.respondInvitation(user,id,"accept".equals(action));}
    @PostMapping("/requests/{id}/{action}") public TeamResources.TeamRequest request(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@PathVariable String action){if(!List.of("approve","reject").contains(action))throw new IllegalArgumentException("Acción de solicitud inválida");return service.respondApplication(user,id,"approve".equals(action));}
    @DeleteMapping("/{id}/members/{userId}") public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@PathVariable Long userId){service.removeMember(user,id,userId);return ResponseEntity.noContent().build();}
    @PostMapping("/{id}/leave") public ResponseEntity<Void> leave(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id){service.leave(user,id);return ResponseEntity.noContent().build();}
    @PostMapping("/{id}/captain") public TeamResources.Team transfer(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestBody TeamResources.TransferCaptain body){return service.transfer(user,id,body.userId());}
    @PostMapping("/{id}/challenges") public TeamResources.Challenge challenge(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@RequestBody TeamResources.CreateChallenge body){return service.challenge(user,id,body);}
    @PostMapping("/challenges/{id}/{action}") public TeamResources.Challenge challengeAction(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long id,@PathVariable String action){return service.respondChallenge(user,id,action.toUpperCase());}
}

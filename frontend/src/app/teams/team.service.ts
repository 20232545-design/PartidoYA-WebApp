import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_BASE_URL } from '@shared/services/api-base-url.token';
import { MyTeams, PublicPlayer, SaveTeam, Team, TeamChallenge, TeamMember, TeamPage, TeamPlayer, TeamRequest } from './team.models';

@Injectable({providedIn:'root'})
export class TeamService {
  private readonly http=inject(HttpClient); private readonly base=`${inject(API_BASE_URL)}/teams`;
  search(filters:{search?:string;sport?:string;district?:string;level?:string;open?:boolean;page?:number;size?:number}){let p=new HttpParams();Object.entries(filters).forEach(([k,v])=>{if(v!==undefined&&v!==null&&v!=='')p=p.set(k,String(v));});return this.http.get<TeamPage>(this.base,{params:p});}
  get(id:number){return this.http.get<Team>(`${this.base}/${id}`);} members(id:number){return this.http.get<TeamMember[]>(`${this.base}/${id}/members`);} mine(){return this.http.get<MyTeams>(`${this.base}/mine`);}
  create(body:SaveTeam){return this.http.post<Team>(this.base,body);} update(id:number,body:SaveTeam){return this.http.put<Team>(`${this.base}/${id}`,body);} deactivate(id:number){return this.http.delete<void>(`${this.base}/${id}`);}
  uploadLogo(id:number,file:File){const body=new FormData();body.append('file',file);return this.http.post<{logoUrl:string}>(`${this.base}/${id}/logo`,body);}
  players(id:number,search:string){return this.http.get<TeamPlayer[]>(`${this.base}/${id}/players`,{params:{search}});} invite(id:number,userId:number){return this.http.post<TeamRequest>(`${this.base}/${id}/invitations`,{userId});}
  publicPlayer(userId:number){return this.http.get<PublicPlayer>(`${this.base}/players/${userId}`);}
  apply(id:number){return this.http.post<TeamRequest>(`${this.base}/${id}/requests`,{});} requests(id:number){return this.http.get<TeamRequest[]>(`${this.base}/${id}/requests`);}
  invitation(id:number,action:'accept'|'reject'){return this.http.post<TeamRequest>(`${this.base}/invitations/${id}/${action}`,{});} request(id:number,action:'approve'|'reject'){return this.http.post<TeamRequest>(`${this.base}/requests/${id}/${action}`,{});}
  remove(id:number,userId:number){return this.http.delete<void>(`${this.base}/${id}/members/${userId}`);} leave(id:number){return this.http.post<void>(`${this.base}/${id}/leave`,{});} transfer(id:number,userId:number){return this.http.post<Team>(`${this.base}/${id}/captain`,{userId});}
  challenge(target:number,challengerTeamId:number,message:string){return this.http.post<TeamChallenge>(`${this.base}/${target}/challenges`,{challengerTeamId,message});} challengeAction(id:number,action:'accepted'|'rejected'|'cancelled'){return this.http.post<TeamChallenge>(`${this.base}/challenges/${id}/${action}`,{});}
}

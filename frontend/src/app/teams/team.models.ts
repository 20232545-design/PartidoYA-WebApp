export interface TeamStatistics { available:boolean; played:number; wins:number; draws:number; losses:number; winPercentage:number; }
export interface Team { id:number; name:string; sport:string; description:string; logoUrl:string|null; district:string|null; competitiveLevel:string; openToRequests:boolean; status:string; captainId:number; captainName:string; memberCount:number; createdAt:string; member:boolean; captain:boolean; pendingRequest:boolean; statistics:TeamStatistics; }
export interface TeamPage { content:Team[]; page:number; size:number; totalElements:number; totalPages:number; }
export interface TeamMember { userId:number; publicName:string; avatarUrl:string|null; role:string; primarySport:string|null; skillLevel:string|null; joinedAt:string; }
export interface TeamPlayer { userId:number; publicName:string; avatarUrl:string|null; skillLevel:string|null; }
export interface PublicPlayer { userId:number; publicName:string; avatarUrl:string|null; district:string|null; primarySport:string|null; skillLevel:string|null; }
export interface TeamRequest { id:number; teamId:number; teamName:string; userId:number; userName:string; type:string; status:string; createdAt:string; expiresAt:string|null; }
export interface TeamChallenge { id:number; challengerTeamId:number; challengerTeamName:string; challengedTeamId:number; challengedTeamName:string; sport:string; message:string|null; status:string; matchId:number|null; createdAt:string; }
export interface MyTeams { administered:Team[]; memberships:Team[]; invitations:TeamRequest[]; challenges:TeamChallenge[]; pendingCount:number; }
export interface SaveTeam { name:string; sport:string; description:string; district:string|null; competitiveLevel:string; openToRequests:boolean; }

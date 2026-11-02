import { AfterViewInit, Component, ElementRef, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent implements AfterViewInit {
  @ViewChild('heroVideo') heroVideo?: ElementRef<HTMLVideoElement>;

  ngAfterViewInit(): void {
    const video = this.heroVideo?.nativeElement;
    if (!video) return;
    video.muted = true;
    video.play().catch(() => {});
  }
}

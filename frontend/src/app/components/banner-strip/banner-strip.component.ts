import { Component } from '@angular/core';
import { RevealDirective } from '../../directives/reveal.directive';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-banner-strip',
  standalone: true,
  imports: [RouterModule, RevealDirective],
  templateUrl: './banner-strip.component.html',
  styleUrls: ['./banner-strip.component.css']
})
export class BannerStripComponent {}

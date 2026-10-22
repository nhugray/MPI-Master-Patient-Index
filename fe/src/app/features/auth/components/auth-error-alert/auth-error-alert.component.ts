import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-error-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './auth-error-alert.component.html',
  styleUrls: ['./auth-error-alert.component.css']
})
export class AuthErrorAlertComponent {
  @Input() message: string | null = null;
}

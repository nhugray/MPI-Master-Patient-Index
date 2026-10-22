import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SearchService } from '../../shared/services/search.service';
import { TokenStorageService } from '../../core/services/token-storage.service';
import { AuthUser } from '../../features/auth/models/auth-user.model';
import { getInitials } from '../../shared/utils/string.util';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  private readonly searchService = inject(SearchService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  searchQuery = '';
  readonly getInitials = getInitials;

  get currentUser(): AuthUser | null {
    return this.tokenStorage.getCurrentUser();
  }

  get userName(): string {
    return this.currentUser?.name ?? '';
  }

  get userRole(): string {
    const roles = this.currentUser?.roles;
    if (!roles || roles.length === 0) return '';
    return roles[0].name;
  }

  get userInitials(): string {
    return getInitials(this.userName);
  }

  onSearch(): void {
    this.searchService.setQuery(this.searchQuery);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }
}

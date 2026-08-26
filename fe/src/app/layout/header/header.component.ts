import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SearchService } from '../../shared/services/search.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  private readonly searchService = inject(SearchService);
  searchQuery = '';

  onSearch(): void {
    this.searchService.setQuery(this.searchQuery);
  }
}
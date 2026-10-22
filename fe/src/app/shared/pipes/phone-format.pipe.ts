import { Pipe, PipeTransform } from '@angular/core';
import { formatPhoneNumber } from '../utils/string.util';

@Pipe({
  name: 'phoneFormat',
  standalone: true
})
export class PhoneFormatPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return formatPhoneNumber(value);
  }
}

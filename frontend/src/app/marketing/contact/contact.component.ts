import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

type ContactField = 'name' | 'email' | 'subject' | 'message';

@Component({
  selector: 'app-contact',
  imports: [FormsModule],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss'
})
export class ContactComponent {
  submitted = false;
  sent = false;

  form: Record<ContactField, string> = {
    name: '',
    email: '',
    subject: '',
    message: ''
  };

  isInvalid(field: ContactField): boolean {
    if (!this.submitted) return false;
    if (field === 'email') {
      return !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email.trim());
    }
    return !this.form[field].trim();
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    this.submitted = true;
    this.sent = false;

    if ((Object.keys(this.form) as ContactField[]).some(field => this.isInvalid(field))) {
      return;
    }

    this.sent = true;
    this.submitted = false;
    this.form = { name: '', email: '', subject: '', message: '' };
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CustomerProfile {
  id?: number;
  email?: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
  kycStatus?: string;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {

  constructor(private http: HttpClient) {}

  saveProfile(profile: CustomerProfile): Observable<any> {
    return this.http.post<any>('/customer-service/api/customer/profile', profile);
  }

  getProfile(): Observable<CustomerProfile> {
    return this.http.get<CustomerProfile>('/customer-service/api/customer/profile');
  }
}

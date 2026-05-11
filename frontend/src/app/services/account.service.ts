import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Account {
  id: string;
  accountNumber: string;
  accountType: string;
  currency: string;
  availableBalance: number;
  currentBalance: number;
  holdAmount: number;
  dailyTransferLimit: number;
  status: string;
  openedAt: string;
}

export interface OpenAccountRequest {
  accountType: string;
  initialDeposit: number;
  currency: string;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private base = '/account-service/api/accounts';

  constructor(private http: HttpClient) {}

  getMyAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(this.base);
  }

  openAccount(request: OpenAccountRequest): Observable<any> {
    return this.http.post<any>(this.base, request);
  }

  getAccount(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.base}/${id}`);
  }

  closeAccount(id: string): Observable<any> {
    return this.http.post<any>(`${this.base}/${id}/close`, {});
  }
}

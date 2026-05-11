import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Transaction {
  id: string;
  referenceNumber: string;
  transactionType: string;
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  amount: number;
  feeAmount: number;
  description: string;
  status: string;
  initiatedAt: string;
  completedAt: string;
}

export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountNumber: string;
  amount: number;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private base = '/transaction-service/api/transactions';

  constructor(private http: HttpClient) {}

  getHistory(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(this.base);
  }

  transfer(request: TransferRequest): Observable<any> {
    return this.http.post<any>(`${this.base}/transfer`, request);
  }

  getById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.base}/${id}`);
  }
}

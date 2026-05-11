import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Loan {
  id: string;
  loanNumber: string;
  loanType: string;
  accountId: string;
  principalAmount: number;
  interestRate: number;
  tenureMonths: number;
  emiAmount: number;
  totalInterest: number;
  totalPayable: number;
  outstandingBalance: number;
  status: string;
  purpose: string;
  appliedAt: string;
  approvedAt: string;
  disbursedAt: string;
}

export interface LoanApplicationRequest {
  accountId: string;
  loanType: string;
  principalAmount: number;
  tenureMonths: number;
  purpose: string;
}

export interface EmiCalculation {
  principal: number;
  tenureMonths: number;
  annualInterestRate: number;
  monthlyEmi: number;
  totalInterest: number;
  totalPayable: number;
}

@Injectable({ providedIn: 'root' })
export class LoanService {
  private base = '/loan-service/api/loans';

  constructor(private http: HttpClient) {}

  getMyLoans(): Observable<Loan[]> {
    return this.http.get<Loan[]>(this.base);
  }

  applyForLoan(request: LoanApplicationRequest): Observable<any> {
    return this.http.post<any>(`${this.base}/apply`, request);
  }

  getLoan(id: string): Observable<Loan> {
    return this.http.get<Loan>(`${this.base}/${id}`);
  }

  calculateEmi(principal: number, tenureMonths: number): Observable<EmiCalculation> {
    const params = new HttpParams()
      .set('principal', principal.toString())
      .set('tenureMonths', tenureMonths.toString());
    return this.http.post<EmiCalculation>(`${this.base}/calculate`, null, { params });
  }
}

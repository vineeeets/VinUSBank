import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Card {
  id: string;
  cardNumberMasked: string;
  cardNumberLastFour: string;
  cardholderName: string;
  cardType: string;
  expiryMonth: number;
  expiryYear: number;
  dailySpendLimit: number;
  onlineEnabled: boolean;
  internationalEnabled: boolean;
  status: string;
  activatedAt: string;
}

export interface CardRequest {
  accountId: string;
  cardholderName: string;
}

@Injectable({ providedIn: 'root' })
export class CardService {
  private base = '/card-service/api/cards';

  constructor(private http: HttpClient) {}

  getMyCards(): Observable<Card[]> {
    return this.http.get<Card[]>(this.base);
  }

  requestCard(request: CardRequest): Observable<any> {
    return this.http.post<any>(this.base, request);
  }

  activateCard(id: string): Observable<any> {
    return this.http.post<any>(`${this.base}/${id}/activate`, {});
  }

  blockCard(id: string, reason?: string): Observable<any> {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.http.post<any>(`${this.base}/${id}/block${params}`, {});
  }

  unblockCard(id: string): Observable<any> {
    return this.http.post<any>(`${this.base}/${id}/unblock`, {});
  }
}

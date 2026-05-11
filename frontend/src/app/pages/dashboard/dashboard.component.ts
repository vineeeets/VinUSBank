import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CustomerService, CustomerProfile } from '../../services/customer.service';
import { AuthService } from '../../services/auth.service';
import { AccountService, Account } from '../../services/account.service';
import { TransactionService, Transaction } from '../../services/transaction.service';
import { LoanService, Loan, EmiCalculation } from '../../services/loan.service';
import { CardService, Card } from '../../services/card.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  // ── Navigation ────────────────────────────────────────────
  activeTab: 'overview' | 'accounts' | 'transfers' | 'loans' | 'cards' = 'overview';
  userEmail = '';

  // ── Profile (Overview tab) ────────────────────────────────
  profile: CustomerProfile | null = null;
  isLoading = false;
  isSaving = false;
  loadError = '';
  saveError = '';
  saveSuccess = '';
  form: CustomerProfile = { firstName: '', lastName: '', phoneNumber: '', address: '' };

  // ── Accounts tab ──────────────────────────────────────────
  accounts: Account[] = [];
  accountsLoading = false;
  showOpenAccountForm = false;
  newAccount = { accountType: 'CHECKING', initialDeposit: 0, currency: 'USD' };
  accountMsg = '';
  accountError = '';

  // ── Transfers tab ─────────────────────────────────────────
  transactions: Transaction[] = [];
  txnLoading = false;
  txnError = '';
  showTransferForm = false;
  transferForm = { sourceAccountId: '', destinationAccountNumber: '', amount: 0, description: '' };
  transferMsg = '';
  transferError = '';
  transferLoading = false;

  // ── Loans tab ─────────────────────────────────────────────
  loans: Loan[] = [];
  loansLoading = false;
  showLoanForm = false;
  loanForm = { accountId: '', loanType: 'PERSONAL', principalAmount: 5000, tenureMonths: 12, purpose: '' };
  emiCalc: EmiCalculation | null = null;
  loanMsg = '';
  loanError = '';
  loanLoading = false;

  // ── Cards tab ─────────────────────────────────────────────
  cards: Card[] = [];
  cardsLoading = false;
  showCardForm = false;
  cardForm = { accountId: '', cardholderName: '' };
  cardMsg = '';
  cardError = '';

  constructor(
    private customerService: CustomerService,
    private authService: AuthService,
    private accountService: AccountService,
    private transactionService: TransactionService,
    private loanService: LoanService,
    private cardService: CardService
  ) {}

  ngOnInit(): void {
    this.userEmail = this.authService.getEmailFromToken() || '';
    this.loadProfile();
    this.loadAccounts();
    this.loadTransactions();
    this.loadLoans();
    this.loadCards();
  }

  // ─── Tab navigation ───────────────────────────────────────
  setTab(tab: 'overview' | 'accounts' | 'transfers' | 'loans' | 'cards'): void {
    this.activeTab = tab;
  }

  logout(): void { this.authService.logout(); }
  getKycClass(): string { return this.profile?.kycStatus?.toLowerCase() || 'pending'; }

  // ─── Profile ──────────────────────────────────────────────
  loadProfile(): void {
    this.isLoading = true;
    this.customerService.getProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.form = { firstName: data.firstName, lastName: data.lastName, phoneNumber: data.phoneNumber, address: data.address };
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; this.profile = null; }
    });
  }

  saveProfile(): void {
    if (!this.form.firstName || !this.form.lastName) { this.saveError = 'First Name and Last Name are required.'; return; }
    this.isSaving = true; this.saveError = ''; this.saveSuccess = '';
    this.customerService.saveProfile(this.form).subscribe({
      next: (res) => {
        this.isSaving = false; this.saveSuccess = 'Profile saved!';
        this.profile = res.profile;
        setTimeout(() => this.saveSuccess = '', 4000);
      },
      error: (err) => { this.isSaving = false; this.saveError = err.error?.message || 'Failed to save.'; }
    });
  }

  // ─── Accounts ─────────────────────────────────────────────
  loadAccounts(): void {
    this.accountsLoading = true;
    this.accountService.getMyAccounts().subscribe({
      next: (data) => { this.accounts = data; this.accountsLoading = false; },
      error: () => { this.accountsLoading = false; }
    });
  }

  openNewAccount(): void {
    this.accountError = ''; this.accountMsg = '';
    this.accountService.openAccount(this.newAccount).subscribe({
      next: (res) => {
        this.accountMsg = `Account ${res.account.accountNumber} opened successfully!`;
        this.showOpenAccountForm = false;
        this.newAccount = { accountType: 'CHECKING', initialDeposit: 0, currency: 'USD' };
        this.loadAccounts();
        setTimeout(() => this.accountMsg = '', 5000);
      },
      error: (err) => { this.accountError = err.error?.message || 'Failed to open account.'; }
    });
  }

  getTotalBalance(): number {
    return this.accounts
      .filter(a => a.status === 'ACTIVE')
      .reduce((sum, a) => sum + a.availableBalance, 0);
  }

  getAccountTypeIcon(type: string): string {
    switch (type) {
      case 'CHECKING': return '🏦';
      case 'SAVINGS': return '💰';
      case 'MONEY_MARKET': return '📈';
      case 'CD': return '🔒';
      default: return '💳';
    }
  }

  // ─── Transactions ─────────────────────────────────────────
  loadTransactions(): void {
    this.txnLoading = true;
    this.transactionService.getHistory().subscribe({
      next: (data) => { this.transactions = data; this.txnLoading = false; },
      error: () => { this.txnLoading = false; }
    });
  }

  makeTransfer(): void {
    this.transferError = ''; this.transferMsg = ''; this.transferLoading = true;
    this.transactionService.transfer(this.transferForm).subscribe({
      next: (res) => {
        this.transferLoading = false;
        this.transferMsg = `Transfer ${res.transaction.referenceNumber} completed!`;
        this.showTransferForm = false;
        this.transferForm = { sourceAccountId: '', destinationAccountNumber: '', amount: 0, description: '' };
        this.loadAccounts();
        this.loadTransactions();
        setTimeout(() => this.transferMsg = '', 5000);
      },
      error: (err) => { this.transferLoading = false; this.transferError = err.error?.message || 'Transfer failed.'; }
    });
  }

  // ─── Loans ────────────────────────────────────────────────
  loadLoans(): void {
    this.loansLoading = true;
    this.loanService.getMyLoans().subscribe({
      next: (data) => { this.loans = data; this.loansLoading = false; },
      error: () => { this.loansLoading = false; }
    });
  }

  calculateEmi(): void {
    if (this.loanForm.principalAmount > 0 && this.loanForm.tenureMonths > 0) {
      this.loanService.calculateEmi(this.loanForm.principalAmount, this.loanForm.tenureMonths).subscribe({
        next: (data) => { this.emiCalc = data; },
        error: () => { this.emiCalc = null; }
      });
    }
  }

  applyForLoan(): void {
    this.loanError = ''; this.loanMsg = ''; this.loanLoading = true;
    this.loanService.applyForLoan(this.loanForm).subscribe({
      next: (res) => {
        this.loanLoading = false;
        const status = res.loan.status;
        this.loanMsg = status === 'DISBURSED'
          ? `Loan ${res.loan.loanNumber} approved & $${res.loan.principalAmount} disbursed to your account!`
          : `Loan ${res.loan.loanNumber} submitted for review.`;
        this.showLoanForm = false;
        this.emiCalc = null;
        this.loanForm = { accountId: '', loanType: 'PERSONAL', principalAmount: 5000, tenureMonths: 12, purpose: '' };
        this.loadLoans();
        this.loadAccounts();
        setTimeout(() => this.loanMsg = '', 6000);
      },
      error: (err) => { this.loanLoading = false; this.loanError = err.error?.message || 'Loan application failed.'; }
    });
  }

  getLoanStatusClass(status: string): string {
    switch (status) {
      case 'DISBURSED': case 'ACTIVE': return 'status-active';
      case 'APPLIED': case 'UNDER_REVIEW': return 'status-pending';
      case 'REJECTED': case 'CLOSED': return 'status-closed';
      default: return '';
    }
  }

  // ─── Cards ────────────────────────────────────────────────
  loadCards(): void {
    this.cardsLoading = true;
    this.cardService.getMyCards().subscribe({
      next: (data) => { this.cards = data; this.cardsLoading = false; },
      error: () => { this.cardsLoading = false; }
    });
  }

  requestCard(): void {
    this.cardError = ''; this.cardMsg = '';
    this.cardService.requestCard(this.cardForm).subscribe({
      next: (res) => {
        this.cardMsg = `Card ${res.card.cardNumberMasked} issued! Please activate it.`;
        this.showCardForm = false;
        this.cardForm = { accountId: '', cardholderName: '' };
        this.loadCards();
        setTimeout(() => this.cardMsg = '', 5000);
      },
      error: (err) => { this.cardError = err.error?.message || 'Card request failed.'; }
    });
  }

  activateCard(id: string): void {
    this.cardService.activateCard(id).subscribe({
      next: () => { this.cardMsg = 'Card activated successfully!'; this.loadCards(); setTimeout(() => this.cardMsg = '', 4000); },
      error: (err) => { this.cardError = err.error?.message || 'Activation failed.'; }
    });
  }

  blockCard(id: string): void {
    this.cardService.blockCard(id, 'Blocked by cardholder').subscribe({
      next: () => { this.cardMsg = 'Card blocked.'; this.loadCards(); },
      error: (err) => { this.cardError = err.error?.message || 'Block failed.'; }
    });
  }

  unblockCard(id: string): void {
    this.cardService.unblockCard(id).subscribe({
      next: () => { this.cardMsg = 'Card unblocked.'; this.loadCards(); },
      error: (err) => { this.cardError = err.error?.message || 'Unblock failed.'; }
    });
  }

  getCardStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'PENDING': return 'status-pending';
      case 'BLOCKED': return 'status-blocked';
      default: return 'status-closed';
    }
  }
}

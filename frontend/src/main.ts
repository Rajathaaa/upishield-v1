import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface FraudResponse {
  transactionId: number;
  fraudProbability: number;
  anomalyScore: number;
  riskScore: number;
  riskLevel: string;
  decision: string;
}

interface Dashboard {
  totalTransactions: number;
  highRisk: number;
  openAlerts: number;
  mediumRisk: number;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="page">
    <header>
      <div>
        <h1>UPIShield</h1>
        <p>UPI Fraud Detection — Version 1</p>
      </div>
      <button (click)="loadDashboard()">Refresh</button>
    </header>

    <section class="cards">
      <div class="card">
        <span>Total Transactions</span>
        <strong>{{ dashboard.totalTransactions }}</strong>
      </div>
      <div class="card">
        <span>High Risk</span>
        <strong>{{ dashboard.highRisk }}</strong>
      </div>
      <div class="card">
        <span>Open Alerts</span>
        <strong>{{ dashboard.openAlerts }}</strong>
      </div>
      <div class="card">
        <span>Medium Risk</span>
        <strong>{{ dashboard.mediumRisk }}</strong>
      </div>
    </section>

    <main>
      <section class="panel">
        <h2>Analyze Transaction</h2>

        <form (ngSubmit)="analyze()">
          <label>User ID
            <input type="number" [(ngModel)]="form.userId" name="userId">
          </label>

          <label>Amount
            <input type="number" [(ngModel)]="form.amount" name="amount">
          </label>

          <label>Merchant ID
            <input [(ngModel)]="form.merchantId" name="merchantId">
          </label>

          <label>Device ID
            <input [(ngModel)]="form.deviceId" name="deviceId">
          </label>

          <label>Hour (0–23)
            <input type="number" min="0" max="23"
                   [(ngModel)]="form.hour" name="hour">
          </label>

          <label>Transactions in last 5 min
            <input type="number"
                   [(ngModel)]="form.transactionsLast5Min"
                   name="transactionsLast5Min">
          </label>

          <label>Average user amount
            <input type="number"
                   [(ngModel)]="form.avgUserAmount"
                   name="avgUserAmount">
          </label>

          <label class="check">
            <input type="checkbox"
                   [(ngModel)]="form.newDevice"
                   name="newDevice">
            New device
          </label>

          <label class="check">
            <input type="checkbox"
                   [(ngModel)]="form.newBeneficiary"
                   name="newBeneficiary">
            New beneficiary
          </label>

          <button class="primary" type="submit" [disabled]="loading">
            {{ loading ? 'Analyzing...' : 'Analyze Transaction' }}
          </button>
        </form>
      </section>

      <section class="panel result" *ngIf="result">
        <h2>Fraud Analysis</h2>

        <div class="risk" [class.high]="result.riskLevel === 'HIGH'"
             [class.medium]="result.riskLevel === 'MEDIUM'">
          {{ result.riskLevel }}
        </div>

        <div class="score">
          <span>Risk Score</span>
          <strong>{{ (result.riskScore * 100).toFixed(1) }}%</strong>
        </div>

        <div class="metric">
          <span>Fraud probability</span>
          <b>{{ (result.fraudProbability * 100).toFixed(1) }}%</b>
        </div>

        <div class="metric">
          <span>Anomaly score</span>
          <b>{{ (result.anomalyScore * 100).toFixed(1) }}%</b>
        </div>

        <div class="decision">
          Decision: <strong>{{ result.decision }}</strong>
        </div>

        <p class="explain">
          Risk = 70% supervised fraud probability + 30% anomaly score.
        </p>
      </section>

      <section class="panel">
        <h2>Latest Transactions</h2>

        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Amount</th>
              <th>Merchant</th>
              <th>Time</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let tx of transactions">
              <td>{{ tx.id }}</td>
              <td>₹{{ tx.amount | number:'1.0-2' }}</td>
              <td>{{ tx.merchantId }}</td>
              <td>{{ tx.createdAt | date:'short' }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>

    <p class="error" *ngIf="error">{{ error }}</p>
  </div>
  `,
  styles: [`
    * { box-sizing: border-box; }
    body { margin: 0; font-family: Arial, sans-serif; }
    .page { min-height: 100vh; background: #f5f7fb; color: #172033; }
    header {
      padding: 24px 6%; background: #111827; color: white;
      display: flex; justify-content: space-between; align-items: center;
    }
    header h1 { margin: 0 0 4px; }
    header p { margin: 0; opacity: .75; }
    header button, .primary {
      border: 0; padding: 11px 18px; border-radius: 8px;
      cursor: pointer; background: #2563eb; color: white;
    }
    .cards {
      display: grid; grid-template-columns: repeat(4, 1fr);
      gap: 16px; padding: 24px 6%;
    }
    .card, .panel {
      background: white; border-radius: 12px; padding: 20px;
      box-shadow: 0 2px 12px rgba(0,0,0,.06);
    }
    .card span { display: block; color: #64748b; font-size: 13px; }
    .card strong { display: block; margin-top: 8px; font-size: 28px; }
    main {
      padding: 0 6% 40px;
      display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
    }
    .panel:last-child { grid-column: 1 / -1; }
    h2 { margin-top: 0; }
    form { display: grid; gap: 12px; }
    label { display: grid; gap: 6px; font-size: 13px; color: #475569; }
    input {
      border: 1px solid #cbd5e1; border-radius: 7px;
      padding: 10px; font-size: 14px;
    }
    .check { display: flex; grid-template-columns: auto 1fr; align-items: center; }
    .check input { width: auto; }
    .risk {
      display: inline-block; padding: 8px 14px; border-radius: 20px;
      background: #dcfce7; color: #166534; font-weight: bold;
    }
    .risk.medium { background: #fef3c7; color: #92400e; }
    .risk.high { background: #fee2e2; color: #991b1b; }
    .score { margin: 25px 0; }
    .score span { color: #64748b; display: block; }
    .score strong { font-size: 40px; }
    .metric {
      display: flex; justify-content: space-between;
      padding: 12px 0; border-bottom: 1px solid #e2e8f0;
    }
    .decision { margin-top: 20px; font-size: 18px; }
    .explain { color: #64748b; font-size: 13px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: left; }
    th { color: #64748b; font-size: 13px; }
    .error {
      position: fixed; bottom: 20px; right: 20px;
      background: #fee2e2; color: #991b1b;
      padding: 12px 16px; border-radius: 8px;
    }
    @media (max-width: 800px) {
      .cards { grid-template-columns: repeat(2, 1fr); }
      main { grid-template-columns: 1fr; }
      .panel:last-child { grid-column: auto; }
    }
  `]
})
class AppComponent {
  private http = inject(HttpClient);

  dashboard: Dashboard = {
    totalTransactions: 0,
    highRisk: 0,
    openAlerts: 0,
    mediumRisk: 0
  };

  transactions: any[] = [];
  result: FraudResponse | null = null;
  loading = false;
  error = '';

  form = {
    userId: 1,
    amount: 50000,
    merchantId: 'M1001',
    deviceId: 'D_NEW',
    hour: 2,
    transactionsLast5Min: 7,
    avgUserAmount: 1200,
    newDevice: true,
    newBeneficiary: true
  };

  constructor() {
    this.loadDashboard();
    this.loadTransactions();
  }

  loadDashboard() {
    this.http.get<Dashboard>('/api/v1/dashboard/summary')
      .subscribe({
        next: data => this.dashboard = data,
        error: () => this.error = 'Could not connect to Spring Boot.'
      });
  }

  loadTransactions() {
    this.http.get<any[]>('/api/v1/transactions')
      .subscribe({
        next: data => this.transactions = data,
        error: () => this.error = 'Could not load transactions.'
      });
  }

  analyze() {
    this.loading = true;
    this.error = '';

    this.http.post<FraudResponse>('/api/v1/transactions', this.form)
      .subscribe({
        next: data => {
          this.result = data;
          this.loading = false;
          this.loadDashboard();
          this.loadTransactions();
        },
        error: err => {
          this.error = err?.error?.error ?? 'Transaction analysis failed.';
          this.loading = false;
        }
      });
  }
}

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient()]
});

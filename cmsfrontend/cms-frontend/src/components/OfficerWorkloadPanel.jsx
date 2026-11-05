import { workloadLevel, workloadThresholds } from '../services/workloadUtils'
import '../styles/officer-workload.css'

function OfficerWorkloadPanel({ workloads, isLoading, error, onRetry }) {
  return <section className="officer-workload-panel" aria-labelledby="officer-workload-heading">
    <div className="management-header"><div><h2 id="officer-workload-heading">Case officer workloads</h2><p>Active complaints only: assigned and in progress. Every officer can handle every category.</p></div><button className="button button-secondary" type="button" onClick={onRetry} disabled={isLoading}>Refresh workloads</button></div>
    <p className="workload-legend">Green: 0–{workloadThresholds.yellowFrom - 1} · Yellow: {workloadThresholds.yellowFrom}–{workloadThresholds.redFrom - 1} · Red: {workloadThresholds.redFrom}+</p>
    {isLoading ? <div className="dashboard-state" role="status">Loading officer workloads…</div>
      : error ? <div className="dashboard-state dashboard-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={onRetry}>Try again</button></div>
        : workloads.length ? <ul className="officer-workload-list">{workloads.map((officer) => {
          const level = workloadLevel(officer.totalActive)
          return <li key={officer.userId}>
            <div className="workload-identity"><strong>{officer.firstName} {officer.lastName}</strong><span>{officer.employeeNumber}</span></div>
            <dl><div><dt>Assigned</dt><dd>{officer.assignedCount}</dd></div><div><dt>In progress</dt><dd>{officer.inProgressCount}</dd></div><div><dt>Total active</dt><dd>{officer.totalActive}</dd></div></dl>
            <span className={`workload-indicator workload-${level.key}`} aria-label={`${level.label} workload: ${officer.totalActive} active complaints`}>{level.label}</span>
          </li>
        })}</ul> : <div className="dashboard-state"><p>No case officers have been created yet.</p></div>}
  </section>
}

export default OfficerWorkloadPanel

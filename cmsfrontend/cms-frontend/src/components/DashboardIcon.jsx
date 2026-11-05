const paths = {
  dashboard: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  report: 'M12 5v14 M5 12h14',
  complaints: 'M8 3h12v18H4V7 M8 3v4H4 M8 11h8 M8 15h8',
  escalations: 'M12 3v12 M8 7l4-4 4 4 M4 17h16v4H4z',
  officer: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M20 7v6 M17 10h6',
  profile: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2 M16 5a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  home: 'm3 10 9-7 9 7 M5 9v12h14V9 M9 21v-8h6v8',
  menu: 'M4 6h16 M4 12h16 M4 18h16',
  close: 'm6 6 12 12 M18 6 6 18',
  logout: 'M9 4H4v16h5 M13 8l4 4-4 4 M8 12h13',
  chevron: 'm14 6-6 6 6 6',
}

function DashboardIcon({ name, ...props }) {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name] || paths.dashboard} /></svg>
}

export default DashboardIcon

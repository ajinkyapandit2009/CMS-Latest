import { Link } from 'react-router-dom'

function Footer() {
  return <footer className="site-footer"><div className="container footer-grid"><div><Link className="brand brand-footer" to="/" aria-label="CMS home"><span className="brand-mark" aria-hidden="true">C</span><span>CMS</span></Link><p className="footer-copy">A simple, structured way to report and manage civic issues.</p></div><div><h2>Explore</h2><Link to="/">Home</Link><a href="/#how-it-works">How it works</a><a href="/#about">About CMS</a></div><div><h2>Account</h2><Link to="/login">Login</Link><Link to="/register">Citizen registration</Link></div></div><div className="container footer-bottom">© {new Date().getFullYear()} CMS. Built for better neighbourhoods.</div></footer>
}

export default Footer

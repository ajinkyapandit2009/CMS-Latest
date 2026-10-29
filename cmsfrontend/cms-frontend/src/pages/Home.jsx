import { Link } from 'react-router-dom'
import drainImage from '../assets/civic/clogged-drain.png'
import footpathImage from '../assets/civic/damaged-footpath.png'
import signalImage from '../assets/civic/damaged-signal.png'
import heroImage from '../assets/civic/hero-neighbourhood.png'
import garbageImage from '../assets/civic/overflowing-garbage.png'
import potholeImage from '../assets/civic/pothole.png'
import waterloggingImage from '../assets/civic/waterlogging.png'

const issues = [
  { title: 'Overflowing Garbage', text: 'Uncollected waste can affect sanitation, comfort, and the shared spaces we all use.', image: garbageImage, alt: 'Overflowing public garbage bin on a neighbourhood street' },
  { title: 'Potholes & Damaged Roads', text: 'Damaged roads make everyday travel less safe for people walking, cycling, and driving.', image: potholeImage, alt: 'Pothole on a residential road' },
  { title: 'Waterlogging', text: 'Standing water can make routes difficult to use and disrupt life after rainfall.', image: waterloggingImage, alt: 'Waterlogged neighbourhood lane after rainfall' },
  { title: 'Damaged Footpaths', text: 'Uneven paths can create barriers for pedestrians, older people, and wheelchair users.', image: footpathImage, alt: 'Broken paving slabs on a footpath' },
  { title: 'Clogged Drains', text: 'Blocked drains can contribute to water build-up and unhygienic public spaces.', image: drainImage, alt: 'Roadside drain clogged with leaves and debris' },
  { title: 'Damaged Traffic Signals', text: 'Non-functional signals can make crossings harder to navigate safely.', image: signalImage, alt: 'Non-functional traffic signal at an intersection' },
]

const comparisons = [
  { title: 'A cleaner street', before: garbageImage, after: heroImage, beforeAlt: 'Illustrative street with overflowing garbage', afterAlt: 'Illustrative clean neighbourhood street' },
  { title: 'A safer road', before: potholeImage, after: heroImage, beforeAlt: 'Illustrative pothole-damaged road', afterAlt: 'Illustrative maintained neighbourhood road' },
  { title: 'A clearer route', before: waterloggingImage, after: heroImage, beforeAlt: 'Illustrative waterlogged road', afterAlt: 'Illustrative clear neighbourhood street' },
]

function Home() {
  return <>
    <section className="home-hero"><div className="container hero-layout"><div className="hero-copy"><p className="eyebrow">Civic Management System</p><h1>See a Problem? Let&apos;s Fix It Together.</h1><p>From potholes and overflowing garbage to waterlogging and damaged footpaths, report civic issues in your neighbourhood and help build a cleaner, safer, more accessible community.</p><div className="button-row hero-actions"><Link className="button button-primary" to="/login">Raise a Complaint <span aria-hidden="true">→</span></Link><a className="button button-secondary" href="#civic-issues">Explore Civic Issues</a></div></div><div className="hero-visual"><img src={heroImage} alt="Residents on a green urban street looking at a civic infrastructure concern" /><div className="hero-caption"><span aria-hidden="true">●</span> Notice. Report. Follow progress.</div></div></div></section>

    <section className="issues-section section-space" id="civic-issues"><div className="container"><div className="section-heading"><p className="eyebrow">What can be reported</p><h2>Small Issues. Big Impact.</h2><p>Everyday civic problems affect how we live, travel, and experience our neighbourhoods.</p></div><div className="issues-grid">{issues.map((issue) => <article className="issue-card" key={issue.title}><img src={issue.image} alt={issue.alt} loading="lazy" /><div><h3>{issue.title}</h3><p>{issue.text}</p></div></article>)}</div></div></section>

    <section className="difference-section section-space" id="difference"><div className="container"><div className="section-heading section-heading-left"><p className="eyebrow">An illustrative concept</p><h2>Imagine the Difference.</h2><p>A cleaner street, a safer road, a clear footpath. Every improvement starts with a problem being noticed.</p></div><div className="comparison-grid">{comparisons.map((comparison) => <article className="comparison-card" key={comparison.title}><h3>{comparison.title}</h3><div className="comparison-images"><figure><img src={comparison.before} alt={comparison.beforeAlt} loading="lazy" /><figcaption>Before</figcaption></figure><figure><img src={comparison.after} alt={comparison.afterAlt} loading="lazy" /><figcaption>After</figcaption></figure></div></article>)}</div><p className="illustration-note">These are illustrative comparisons, not verified CMS outcomes.</p></div></section>

    <section className="how-section section-space" id="how-it-works"><div className="container"><div className="section-heading"><p className="eyebrow">A simple process</p><h2>From observation to action.</h2></div><ol className="steps-list"><li><span className="step-icon" aria-hidden="true">⌕</span><div><p className="step-number">Step 1</p><h3>Spot an Issue</h3><p>Notice a civic problem affecting your neighbourhood.</p></div></li><li><span className="step-icon" aria-hidden="true">+</span><div><p className="step-number">Step 2</p><h3>Raise a Complaint</h3><p>Sign in and report the problem with its location and supporting details.</p></div></li><li><span className="step-icon" aria-hidden="true">↗</span><div><p className="step-number">Step 3</p><h3>Track Progress</h3><p>Follow the complaint&apos;s status as it moves through the reporting process.</p></div></li></ol></div></section>

    <section className="purpose-section section-space" id="about"><div className="container purpose-layout"><div><p className="eyebrow">Why CMS exists</p><h2>Better reporting begins with a clearer path.</h2></div><div><p>Problems such as potholes, overflowing garbage, clogged drains, and unsafe footpaths can affect health, safety, accessibility, and daily life. CMS is a structured digital platform for reporting civic issues and supporting the administrative process of managing complaints.</p><ul className="benefit-list"><li>Simpler reporting for citizens</li><li>Organized complaint information</li><li>Better visibility into complaint status</li><li>Coordination between citizens, administrators, and case officers</li></ul></div></div></section>

    <section className="closing-section"><div className="container closing-card"><p className="eyebrow">Your voice matters</p><h2>Your Neighbourhood Deserves Better.</h2><p>Noticed something that needs attention? Take the first step toward a better neighbourhood.</p><Link className="button button-primary" to="/login">Raise a Complaint <span aria-hidden="true">→</span></Link></div></section>
  </>
}

export default Home

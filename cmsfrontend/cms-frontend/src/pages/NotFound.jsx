import { Link } from 'react-router-dom'
import { PageScaffold } from './Login'
function NotFound() { return <PageScaffold eyebrow="404" title="This page could not be found" description="The address may be incorrect or the page may have moved." action={<Link className="button button-primary" to="/">Return home</Link>} /> }
export default NotFound

import { Link } from 'react-router-dom'
import { PageScaffold } from './Login'
function AccessDenied() { return <PageScaffold eyebrow="Access restricted" title="You do not have access to this page" description="When authentication is connected, CMS will direct each account to the appropriate area." action={<Link className="button button-primary" to="/">Return home</Link>} /> }
export default AccessDenied

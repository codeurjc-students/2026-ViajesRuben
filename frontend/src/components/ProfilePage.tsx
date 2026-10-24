import { useCallback, useEffect, useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import { useAuth } from '../hooks/useAuth';
import { getProfile } from '../services/userService';
import type { User } from '../types';
import { profileErrorMessage } from '../utils/profileForm';
import PasswordCard from './PasswordCard';
import ProfileAvatarCard from './ProfileAvatarCard';
import ProfileDetailsCard from './ProfileDetailsCard';
import './ProfilePage.css';

/** The logged-in user's own profile; every change is also pushed to the auth context. */
export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const userId = user?.id;
  const [profile, setProfile] = useState<User>();
  const [loadError, setLoadError] = useState<string>();

  useEffect(() => {
    if (userId === undefined) return;
    let cancelled = false;
    getProfile(userId)
      .then((loaded) => {
        if (!cancelled) setProfile(loaded);
      })
      .catch((error) => {
        if (!cancelled) setLoadError(profileErrorMessage(error));
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  // The header shows the user of the auth context, so both must change together.
  const apply = useCallback(
    (next: User) => {
      setProfile(next);
      updateUser(next);
    },
    [updateUser],
  );

  return (
    <Container as="main" className="app-main py-4">
      <h1 className="profile-page__title mb-4 text-white">Mi perfil</h1>
      {loadError && <Alert variant="danger">{loadError}</Alert>}
      {!profile && !loadError && (
        <p className="bg-white rounded p-3" role="status">
          Cargando perfil…
        </p>
      )}
      {profile && (
        <Row className="gy-4">
          <Col lg={6} className="d-flex flex-column gap-4">
            <ProfileAvatarCard profile={profile} onChanged={apply} />
            <ProfileDetailsCard profile={profile} onSaved={apply} />
          </Col>
          {profile.role !== 'ADMIN' && (
            <Col lg={6}>
              <PasswordCard userId={profile.id} />
            </Col>
          )}
        </Row>
      )}
    </Container>
  );
}

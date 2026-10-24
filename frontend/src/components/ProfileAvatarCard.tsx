import { useState } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import { deleteAvatar, uploadAvatar } from '../services/userService';
import type { User } from '../types';
import { profileErrorMessage } from '../utils/profileForm';
import ImageUpload from './ImageUpload';
import UserAvatar from './UserAvatar';

interface ProfileAvatarCardProps {
  profile: User;
  /** Called with the profile as it is after the avatar was uploaded, replaced or deleted. */
  onChanged: (profile: User) => void;
}

/** The profile picture with upload/replace and delete; without one the initial is shown. */
export default function ProfileAvatarCard({
  profile,
  onChanged,
}: Readonly<ProfileAvatarCardProps>) {
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string>();

  async function handleDelete() {
    setDeleting(true);
    setError(undefined);
    try {
      await deleteAvatar(profile.id);
      onChanged({ ...profile, avatarId: null });
    } catch (failure) {
      setError(profileErrorMessage(failure));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <Card as="section" className="profile-card" aria-labelledby="profile-avatar-title">
      <h2 id="profile-avatar-title" className="profile-card__title">
        Foto de perfil
      </h2>
      <div className="profile-avatar">
        <UserAvatar user={profile} size="lg" alt="Tu foto de perfil" />
        <div className="profile-avatar__controls">
          <ImageUpload
            label={profile.avatarId ? 'Cambiar foto' : 'Subir foto'}
            preview={false}
            upload={(file) => uploadAvatar(profile.id, file)}
            onUploaded={(image) => {
              setError(undefined);
              onChanged({ ...profile, avatarId: image.id });
            }}
          />
          {profile.avatarId && (
            <Button
              variant="outline-danger"
              size="sm"
              className="mt-3"
              disabled={deleting}
              onClick={handleDelete}
            >
              {deleting ? 'Eliminando…' : 'Eliminar foto'}
            </Button>
          )}
          {error && (
            <Alert variant="danger" className="mt-3 mb-0">
              {error}
            </Alert>
          )}
        </div>
      </div>
    </Card>
  );
}

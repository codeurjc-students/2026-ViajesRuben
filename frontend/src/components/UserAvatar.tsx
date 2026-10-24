import type { User } from '../types';
import StoredImage from './StoredImage';
import './UserAvatar.css';

interface UserAvatarProps {
  user: Pick<User, 'name' | 'avatarId'>;
  /** `sm` for the header, `lg` for the profile page. */
  size?: 'sm' | 'lg';
  /** Description of the picture; empty when the name is written next to it. */
  alt?: string;
}

/** The user's picture, or the initial of their name when they have none. */
export default function UserAvatar({ user, size = 'sm', alt = '' }: Readonly<UserAvatarProps>) {
  const initial = <span aria-hidden="true">{user.name.charAt(0).toUpperCase()}</span>;

  return (
    <span className={`user-avatar user-avatar--${size}`}>
      <StoredImage id={user.avatarId ?? undefined} alt={alt} fallback={initial} />
    </span>
  );
}

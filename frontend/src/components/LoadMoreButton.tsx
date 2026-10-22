import Button from 'react-bootstrap/Button';
import Spinner from 'react-bootstrap/Spinner';

interface LoadMoreButtonProps {
  /** The server has no more pages: the button disappears. */
  last: boolean;
  loading: boolean;
  onClick: () => void;
}

/** "Más resultados" button shared by every paginated list. */
export default function LoadMoreButton({ last, loading, onClick }: LoadMoreButtonProps) {
  if (last) return null;

  return (
    <div className="d-flex justify-content-center py-4">
      <Button
        variant="light"
        size="lg"
        className="fw-semibold"
        disabled={loading}
        onClick={onClick}
      >
        {loading && (
          <Spinner animation="border" size="sm" role="status" aria-hidden="true" className="me-2" />
        )}
        Más resultados
      </Button>
    </div>
  );
}

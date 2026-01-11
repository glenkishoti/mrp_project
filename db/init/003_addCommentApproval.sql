-- Migration: Add comment approval status to ratings table
ALTER TABLE ratings
ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) DEFAULT 'pending';

-- Update existing ratings to 'approved' (grandfather existing data)
UPDATE ratings
SET approval_status = 'approved'
WHERE approval_status IS NULL OR approval_status = 'pending';

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_ratings_approval_status ON ratings(approval_status);

-- Valid values: 'pending', 'approved', 'rejected'
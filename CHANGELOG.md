# Changelog

## [Unreleased]

### Improvements

- Improve handling when rows with different values are selected
- Cache resolved mappings of the grid
- Improve message when no valid cell is selected

### Fixes

- Move action updates back to the background thread

## [1.0.8] - 2026-10-25

### Improvements

- Update to 2026.3

## [1.0.7] - 2026-08-14

### Improvements

- Update to 2026.2

## [1.0.6] - 2026-04-01

### Improvements

- Update to 2026.1

### Fixes

- Fix error when a result table is missing a column which is part of a composite foreign key

## [1.0.5] - 2025-09-24

### Improvements

- Update to 2025.2.2

## [1.0.4] - 2025-09-04

### Improvements

- Update to 2025.2.1

## [1.0.3] - 2025-08-06

### Improvements

- Update to 2025.2

## [1.0.2] - 2025-05-11

### Improvements

- Only reload the lookup table if a different foreign key is followed
- Select row in the lookup table without running any queries if already on the right page

## [1.0.1] - 2025-05-05

### Improvements

- Allow usage in the console query result grid

### Fixes

- Only select the row in the lookup table if it is the right one
- Move lookup action updates to the background thread

## [1.0.0] - 2025-05-03

Initial release

[Unreleased]: https://github.com/jkdhn/dblookup/compare/v1.0.8...HEAD
[1.0.8]: https://github.com/jkdhn/dblookup/compare/v1.0.7...v1.0.8
[1.0.7]: https://github.com/jkdhn/dblookup/compare/v1.0.6...v1.0.7
[1.0.6]: https://github.com/jkdhn/dblookup/compare/v1.0.5...v1.0.6
[1.0.5]: https://github.com/jkdhn/dblookup/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/jkdhn/dblookup/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/jkdhn/dblookup/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/jkdhn/dblookup/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/jkdhn/dblookup/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/jkdhn/dblookup/commits/v1.0.0

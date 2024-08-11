# Sudoku solver

This program uses various methods to solve sukodus. Current methods used:

- Cell can only be one number - when there is only one digit that a cell can be
- Cell is the only candidate for a number - when the cell is the only one in its group that can be a certain digit
- Pairs - two cells have the same two candidates, which excludes all other cells in the group for those two digits
- Triples, fours etc. - the same as above, but for more cells and digits. Currently only checks up to 3, but could easily do more.

## Usage

Clone the repo and run `Main.kt`. Enter your grid as a continuous string, left to right, top to bottom. Use spaces to indicate unknown cells. Once you input a grid once, it caches it for next time in `grid_cache.txt`. No need to enter manually every time!

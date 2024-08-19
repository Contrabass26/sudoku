# Sudoku solver

This program uses various methods to solve sukodus. Current methods used:

- Cell can only be one number - when there is only one digit that a cell can be
- Cell is the only candidate for a number - when the cell is the only one in its group that can be a certain digit
- Pairs - two cells have the same two candidates, which excludes all other cells in the group for those two digits
- Triples, fours etc. - the same as above, but for more cells and digits. Currently only checks up to 3, but could easily do more.
- Pairs of cells that share multiple collections - neither collection can have them anywhere else

I have also done various experiments with automatically detecting and solving a sudoku given an image (in the `recognition` package). Currently, the only one that works is `Tesseract.kt`, however here are the things I tried:

- `Recognition.kt` - search for the corners of the grid by measuring how far each pixel could go in a straight line at various angles. I got this working for one image, but there a lot of parameters to tune, and the optimal values were different for each image.
- `ConvoRecognition.kt` - use a convolution to highlight pixels that look like corners. Didn't work very well, lots of false positives.
- `WeirdConvoRecognition.kt` - a similar approach to the above, followed by a perspective transform to get a nice image of the sudoku. The main new thing here was the perspective transform, which worked well when the corners were correct.
- `LineConvoRecognition.kt` - use a convolution to detect lines instead of corners, then try to find a set of lines that looks likely. This was very involved with a lot of parameters to tune, so it was hard to get a solution that worked for lots of different images.
- `NeuralRecognition.kt` - use a neural network to classify pixels as corners, lines, etc. Never got anywhere, but this may be due to a lack of training data.
- `BalanceRecognition.kt` - map pixels to their "balance" (how symmetrical their neighbours are). Produced some very cool images, but not very helpful.
- `ManualRecognition.kt` - let the user select the corners, then do a perspective transform to get a nice square grid image. Use a model trained on the MNIST dataset to recognise digits in individual cells. The first parts worked perfectly, but the model wasn't expecially good at classifying the digits in the sudoku.
- `Tesseract.kt` - same method as above, but using Tesseract's OCR to classify the digits. This wasn't especially reliable either, so I let the user check it before sending it off to the solver. This results in a very usable program.

## Usage

Clone the repo and run `Main.kt`. Enter your grid as a continuous string, left to right, top to bottom. Use spaces to indicate unknown cells. Once you input a grid once, it caches it for next time in `grid_cache.txt`. No need to enter manually every time!

# ezfuck

ezfuck is a BrainFuck derivative. It uses the same commands but adds many features to make the language more "usable".

## Commands

 - Math operators:
  - `+ - * \`

 - Move the cell pointer left and right:
  - `< >`

 - Move instruction pointer left/right:
  - `{ }`

 - Start/stop loop:
  - `[ ]`

 - Number literals:
  - `0 1 2 3 4 5 6 7 8 9`

 - Set the current cell 's value:
  - `^`

 - Get the current cell's value:
  - `V`

## Features

 - Cells can hold any positive value up to Java's `Long/MAX_VALUE`
 - Cells expand to the right as needed. Cells with a 0 value are also automatically removed from the right.
 - Cells don't "wrap" from left to right. Going left past the first cell is a no-op.
 - Commands (with the exception of `[ ]` and `V`) can take a number as an argument.
  - `>5` is the same as `>>>>>`, while `+++++++++++++++++` can be written as `+17`
 - The "insertion"/"extraction" operators (`V`/`^`) can be used to directly set a cells value, and get the cell's value to be used as an argument for another command.
  - `^5` sets the current cell's value to 5, regardless of what it was before
  - `V` evaluates to the current cells value, immediately before the command on its right is run.
  - `^20 >V` sets the current cell to `20`, then takes that value and gives it to `>`. This would be the same as `>>>>>>>>>>>>>>>>>>>>`. You could also do something like `,>V`, which would move to the right based on user input.
  - The instruction pointer can be manipulated directly using `{` and `}`.

- - - - -

BrainFuck code is valid ezfuck code, providing:

 - The code doesn't contain comments with any of the introduced symbols. Have a number, "^" or "V" in the comments could change the behavior of the code.

 - The code doesn't rely on B


## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar ezfuck-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

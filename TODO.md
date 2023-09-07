# TODO

## P0
1. ZIO Direct `ZIO[_, _, Nothing]`
1. Final chapter list
1. Prose and code in each chapter
    1. All code in markdown
1. Example repo
1. Explore end-notes (inline & extracted or 99_endnotes.md)
    1. Markua endnotes are inserted at the end of the chapter
    1. Markua 0.30 is very picky and doesn't have good error messages
    1. End notes can't contain code
    1. We can instead do a 99_endnotes.md and use crosslinks: http://markua.com/#crosslinks-and-ids-m-
    1. This approach will work with mdoc
1. `runDemo` replacement with `mdoc:ziorun`
1. `runTest` replacement with `mdoc:ziotest`
1. Sometimes error in CI: `genManuscript` `java.nio.file.NoSuchFileException: manuscript/Book.txt`

## P1
1. ZIO Direct returning concrete values
1. Booker
    1. Implement ReorderExistingApp
    1. Merge AddNewChapterApp & ReorderExistingApp
1. Mermaid processing
    1. We can turn mermaid blocks into images on manuscript generation

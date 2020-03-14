# Website

This repository contains static data needed for the website.

## How generate pages
We have a kscript to generate html pages.

This script will download data from the [data repo](https://github.com/fp-in-bo/data) and generate a page for each talk.

All you have to do is:

    scripts/generateSinglePages.kts template.html


Once merged to master the new files will be automatically available thanks to [GitHub Pages](https://pages.github.com/).

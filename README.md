# Database Foreign Key Lookup

Adds a Lookup panel which allows looking up and choosing foreign key targets.

![Image](https://plugins.jetbrains.com/files/27270/screenshot_c401ccce-5796-4886-a6f6-138bc3a02f7d)

* Shows the foreign key target in another grid side-by-side
    * Select a row in that grid to choose it
    * The selected cells in the source table are edited to point to the new target
* Allows quickly viewing and editing data in the target table
* Works with multi-column foreign keys
    * Select the columns you want to change
    * The other columns are automatically added to the `WHERE` condition
* Works with large tables
    * Supports pagination for the foreign key target
    * Automatically navigates to the correct page

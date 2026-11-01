<!-- jarbase:start -->
## Documentation (jarbase)

This project's docs live in `docs/`. Docs ship in the same push as the code they describe.
Each page lists the code it describes in its `sources:` frontmatter.

Before you push a branch or open a pull request:

1. Run `sh .jarbase/jarbase affected --uncommitted` to list the docs pages your change affects.
2. Update each affected page. Keep unrelated text untouched.
3. Commit the docs together with the code and push.

You do not need to ask the user to approve the docs change first: update the pages, commit them
together with the code and push. Say in your summary which pages you updated.
If a change needs no docs update, say why and push again: a push is only blocked once.
Never write secrets or configuration values into the docs.
<!-- jarbase:end -->

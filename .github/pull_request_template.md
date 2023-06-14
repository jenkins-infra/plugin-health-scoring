<!-- Comment:
 Please start by adding a link to an issue if the pull request is trying to solve one.
 You can used keyword to do the linking automatically: https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword.
-->

### Description

Closes #issue-id.

<!-- Comment:
 Provide a clear description of the content of the pull request.
 This includes documentation, link to issues, scenario of executions.
 For UI change, a screenshot of before and after the change is also welcome.
 Make sure you read the contributing guide.
 Please explain how this pull request content will benefit the project.
-->

### Testing done

<!-- Comment:
  if there is no automatic test, please explain what you did to validate
  the bugfix or the improvement.
-->

```[tasklist]
### Submitter checklist

- [ ] If an issue exists, it is well described and linked in the description
- [ ] The description of this pull request is detailed and explain why this pull request is needed
- [ ] The changeset is on a specific branch
  - `feature/` for new feature, or improvements
  - `fix/` for bug fixes
  - `docs/` for any documentation changes
- [ ] If required, the documentation has been updated
- [ ] There is automated tests to cover the code change / addition
    - If there is no test, include an explanation why in the description
- [ ] Run `mvn verify` locally and all tests are passing successfully
  - It is OK to create a pull request which has failing tests if it is created as a draft, is to fix a bug and the first commit is the test to prove the existence of the bug.
- [ ] There is no new warnings (checkstyle nor spotbugs) on the code
```

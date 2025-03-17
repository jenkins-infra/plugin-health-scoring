#!/usr/bin/env zsh
set -euo pipefail

# This script is used to generate the project releases.
# This uses 'git' 'gh', 'jq', 'mvn' and must be used with a Bash version greater than or equal to 4.

cmds=('git' 'gh' 'jq' 'mvn')
for cmd in "${cmds[@]}"; do
    command -v "${cmd}" &> /dev/null
done

if [ "${BASH_VERSINFO[0]}" -lt 4 ]; then
	echo "You **must** at least run a Bash version 4. You are using bash version ${BASH_VERSINFO[0]}."
	echo "If you are on a mac, use brew to install bash and use '$(brew --prefix bash)/bin/bash $0 $*'"
	exit 1
fi

main_branch_status=$(\
    gh api /repos/jenkins-infra/plugin-health-scoring/commits/main/status\
    | jq --raw-output ".state"\
)
if [ "${main_branch_status}" != "success" ]; then
    echo "Main branch status is not successful. Please assess."
    gh browse --repo jenkins-infra/plugin-health-scoring --branch main
fi

gh repo sync\
    --source jenkins-infra/plugin-health-scoring\
    --branch main
git switch main

draft_releases=$(\
    gh release list --json tagName,isDraft\
    | jq '[.[] | select(.isDraft == true)]'\
)

draft_count="$(echo "${draft_releases}" | jq '. | length')"
if [ "${draft_count}" -ne 1 ]; then
    if [ "${draft_count}" -eq 0 ]; then
        echo "# There is no release in draft on GitHub. Please assess."
    fi
    if [ "${draft_count}" -gt 1 ]; then
        echo "# There is too many releases in draft on GitHub. Please assess."
    fi
    gh browse --releases
    exit 1
fi

gh_draft_tag=$(\
    echo "${draft_releases}"\
    | jq --raw-output '.[0].tagName'
)

if [ ${#@} -eq 1 ] ; then
    version="${1}"
    shift
else
    echo "# Using GitHub Release to get the release version."
    version=${gh_draft_tag/#v/}
fi

echo "# Release version will be ${version}."

mvn release:prepare\
  -DreleaseVersion="${version}"\
  -B\
  -DskipTests\
  -Dspotbugs.skip=true\
  -Dspotless.check.skip=true\
  -Darguments="-DskipTests -Dspotbugs.skip=true -Dspotless.check.skip=true"\
  -DlocalCheckout=true\
  -DdeployAtEnd=true\
  -DretryFailedDeploymentCount=3

declare -A props
while IFS='=' read -r key value; do
	props["${key}"]="${value}"
done < <(grep -v "^[[:space:]]*#" "release.properties")

release_tag="${props["scm.tag"]}"
release_title="${release_tag/#v/}"

gh release edit "${gh_draft_tag}"\
    --verify-tag\
    --tag "${release_tag}"\
    --title="${release_title}"\
    --draft=false\
    --latest

mvn -q release:clean

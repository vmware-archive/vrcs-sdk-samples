

# Contributing to vrcs-sdk-samples

The vrcs-sdk-samples project team welcomes contributions from the community. If you wish to contribute code and you have not
signed our contributor license agreement (CLA), our bot will update the issue when you open a Pull Request. For any
questions about the CLA process, please refer to our [FAQ](https://cla.vmware.com/faq).

## Contribution Flow

This is a rough outline of what a contributor's workflow looks like:

- Create a topic branch from where you want to base your work
- Make commits of logical units
- Make sure your commit messages are in the proper format (see below)
- Push your changes to a topic branch in your fork of the repository
- Submit a pull request

Example:

``` shell
git remote add upstream https://github.com/vmware/vrcs-sdk-samples.git
git checkout -b my-new-feature master
git commit -a
git push origin my-new-feature
```

### Staying In Sync With Upstream

When your branch gets out of sync with the vmware/master branch, use the following to update:

``` shell
git checkout my-new-feature
git fetch -a
git pull --rebase upstream master
git push --force-with-lease origin my-new-feature
```

### Updating pull requests

If your PR fails to pass CI or needs changes based on code review, you'll most likely want to squash these changes into
existing commits.

If your pull request contains a single commit or your changes are related to the most recent commit, you can simply
amend the commit.

``` shell
git add .
git commit --amend
git push --force-with-lease origin my-new-feature
```

If you need to squash changes into an earlier commit, you can use:

``` shell
git add .
git commit --fixup <commit>
git rebase -i --autosquash master
git push --force-with-lease origin my-new-feature
```

Be sure to add a comment to the PR indicating your new changes are ready to review, as GitHub does not generate a
notification when you git push.

### Code Style

Please use the following checkstyle.xml file to ensure your code follows our style guidelines

``` xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Puppy Crawl//DTD Check Configuration 1.3//EN" "http://www.puppycrawl.com/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>

    <module name="FileTabCharacter">
        <property name="eachLine" value="true"/>
    </module>

    <module name="TreeWalker">
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="ImportOrder">
            <property name="groups" value="/^javax?\./,/^(?!com\.vmware\.)[a-z]/"/>
            <property name="ordered" value="true"/>
            <property name="separated" value="true"/>
            <property name="option" value="top"/>
        </module>
        <module name="RequireThis">
            <property name="checkMethods" value="false"/>
        </module>
        <module name="NeedBraces"/>
        <module name="OuterTypeFilename"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="WhitespaceAround">
            <property name="allowEmptyConstructors" value="true"/>
            <property name="allowEmptyMethods" value="true"/>
            <property name="allowEmptyTypes" value="true"/>
            <property name="allowEmptyLoops" value="true"/>
        </module>
        <module name="OneStatementPerLine"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="ArrayTypeStyle"/>
        <module name="MissingSwitchDefault"/>
        <module name="FallThrough"/>
        <module name="UpperEll"/>
        <module name="ModifierOrder"/>
        <module name="EmptyLineSeparator">
            <property name="allowNoEmptyLineBetweenFields" value="true"/>
        </module>
        <module name="SeparatorWrap">
            <property name="tokens" value="DOT"/>
            <property name="option" value="nl"/>
        </module>
        <module name="SeparatorWrap">
            <property name="tokens" value="COMMA"/>
            <property name="option" value="EOL"/>
        </module>
        <module name="PackageName">
            <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
            <message key="name.invalidPattern"
                     value="Package name ''{0}'' must match pattern ''{1}''."/>
        </module>
        <module name="TypeName">
            <message key="name.invalidPattern"
                     value="Type name ''{0}'' must match pattern ''{1}''."/>
        </module>
        <module name="Indentation">
            <property name="basicOffset" value="4"/>
            <property name="braceAdjustment" value="0"/>
            <property name="caseIndent" value="0"/>
            <property name="throwsIndent" value="8"/>
            <property name="lineWrappingIndentation" value="8"/>
            <property name="arrayInitIndent" value="4"/>
        </module>
    </module>

    <module name="RegexpHeader">
        <property name="headerFile" value="${checkstyle.header.file}"/>
        <property name="fileExtensions" value="java"/>
    </module>

    <module name="RegexpSingleline">
        <property name="format" value="^\s*\*\s*@author"/>
    </module>

    <module name="RegexpSingleline">
        <property name="format" value="^\s*\*\sCreated by [a-z]+ on .*"/>
    </module>

    <module name="RegexpSingleline">
        <property name="format" value="\s+$"/>
        <property name="message" value="Trailing whitespace"/>
    </module>
</module>
```

### Formatting Commit Messages

We follow the conventions on [How to Write a Git Commit Message](http://chris.beams.io/posts/git-commit/).

Be sure to include any related GitHub issue references in the commit message.  See
[GFM syntax](https://guides.github.com/features/mastering-markdown/#GitHub-flavored-markdown) for referencing issues
and commits.

## Reporting Bugs and Creating Issues

When opening a new issue, try to roughly follow the commit message format conventions above.

## Repository Structure

```
SDK-folder
├── samples                                  <- The directory where contributed samples may be placed
│     └── rest-sample-plugin                 <- An SDK sample that can execute REST calls against a remote server
├── lib                                      (everything in this folder is installed in your Maven cache)
│     └── tile-api.jar                       <- The interfaces which your JAVA based tiles must implement
│     └── tile-api-test.jar                  <- A unit test framework that allows tiles to be executed in a dev environment
│     └── tile-api-test-dependency.jar       <- A dependency jar containing all the classes needed to execute tiles in a dev environment
│     └── vrcs-simple-plugin-archetype.jar   <- A maven archetype of a simple vRCS plug-in
│     └── vrcs-bundle-maven-plugin.jar/.pom  <- A maven plug-in that allows seamless plug-in development using maven commands
```

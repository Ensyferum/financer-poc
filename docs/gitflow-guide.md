# GitFlow Configuration for Financer Project

## 📋 Branch Structure

### Main Branches
- **`master`**: Production-ready code. Only releases and hotfixes merge here
- **`develop`**: Integration branch for features. All features branch from here

### Supporting Branches
- **`feature/*`**: New features (branch from develop, merge to develop)
- **`release/*`**: Release preparation (branch from develop, merge to develop + master)
- **`hotfix/*`**: Critical production fixes (branch from master, merge to develop + master)

## 🚀 GitFlow Workflow

### Feature Development
```bash
# Start new feature
git checkout develop
git pull origin develop
git checkout -b feature/JIRA-123-user-authentication

# Work on feature, make commits
git add .
git commit -m "feat: implement user authentication"

# Finish feature
git checkout develop
git pull origin develop
git checkout feature/JIRA-123-user-authentication
git rebase develop
git checkout develop
git merge --no-ff feature/JIRA-123-user-authentication
git push origin develop
git branch -d feature/JIRA-123-user-authentication
```

### Release Workflow
```bash
# Start release
git checkout develop
git pull origin develop
git checkout -b release/1.1.0

# Prepare release (version bumps, final testing)
git commit -m "chore: bump version to 1.1.0"

# Finish release
git checkout master
git merge --no-ff release/1.1.0
git tag -a v1.1.0 -m "Release version 1.1.0"
git checkout develop
git merge --no-ff release/1.1.0
git push origin master develop --tags
git branch -d release/1.1.0
```

### Hotfix Workflow
```bash
# Start hotfix
git checkout master
git pull origin master
git checkout -b hotfix/1.0.1-critical-security-fix

# Make fix
git commit -m "fix: resolve critical security vulnerability"

# Finish hotfix
git checkout master
git merge --no-ff hotfix/1.0.1-critical-security-fix
git tag -a v1.0.1 -m "Hotfix version 1.0.1"
git checkout develop
git merge --no-ff hotfix/1.0.1-critical-security-fix
git push origin master develop --tags
git branch -d hotfix/1.0.1-critical-security-fix
```

## 🔧 Branch Protection Rules

### Master Branch
- Require pull request reviews (2 reviewers)
- Require status checks (CI/CD pipeline must pass)
- Require branches to be up to date
- Restrict pushes (only admins can push directly)
- Require signed commits

### Develop Branch  
- Require pull request reviews (1 reviewer)
- Require status checks (CI/CD pipeline must pass)
- Allow merge commits and squash merging

## 🏷️ Naming Conventions

### Feature Branches
- `feature/JIRA-123-short-description`
- `feature/add-user-authentication`
- `feature/implement-payment-gateway`

### Release Branches
- `release/1.2.0`
- `release/2.0.0-beta`

### Hotfix Branches
- `hotfix/1.1.1-security-fix`
- `hotfix/1.1.2-critical-bug`

## 📝 Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, etc)
- **refactor**: Code refactoring
- **test**: Adding or updating tests
- **chore**: Maintenance tasks
- **ci**: CI/CD changes
- **perf**: Performance improvements
- **build**: Build system changes

### Examples
```bash
feat(auth): add OAuth2 integration
fix(api): resolve null pointer exception in user service
docs: update API documentation
chore(deps): upgrade Spring Boot to 3.2.0
ci: add automated security scanning
```

## 🎯 Integration with CI/CD

### Automated Workflows
- **develop branch**: Runs full CI pipeline + deploys to development
- **master branch**: Runs full CI pipeline + deploys to production
- **feature branches**: Runs CI pipeline (no deployment)
- **release branches**: Runs CI pipeline + deploys to staging
- **hotfix branches**: Runs CI pipeline + fast-track to production

### Environment Mapping
- `feature/*` → No deployment (CI only)
- `develop` → Development environment
- `release/*` → Staging environment
- `master` → Production environment
- `hotfix/*` → Production environment (expedited)

## 🛡️ Quality Gates

### Before Merge to Develop
- [ ] All tests pass
- [ ] Code coverage > 80%
- [ ] Security scan passes
- [ ] Code review approved
- [ ] Feature branch up to date with develop

### Before Merge to Master
- [ ] All tests pass (including integration tests)
- [ ] Performance tests pass
- [ ] Security audit passes
- [ ] Documentation updated
- [ ] Release notes prepared
- [ ] Staging deployment successful

## 🚨 Emergency Procedures

### Critical Production Issues
1. **Immediate**: Create hotfix branch from master
2. **Fix**: Implement minimal fix with tests
3. **Test**: Verify fix in isolated environment
4. **Deploy**: Fast-track through CI/CD pipeline
5. **Communicate**: Notify stakeholders of resolution
6. **Follow-up**: Merge hotfix to develop and create post-mortem

### Rollback Procedure
```bash
# If deployment fails, rollback using tags
git checkout master
git reset --hard v1.0.0  # Previous stable version
git push origin master --force-with-lease

# Trigger deployment of previous version
```

## 📊 Metrics and Monitoring

### Key Metrics
- Lead time from feature start to production
- Deployment frequency
- Mean time to recovery (MTTR)
- Change failure rate
- Code review time

### Monitoring
- Branch health dashboard
- CI/CD pipeline success rates  
- Deployment success rates
- Automated quality gates status

## 🔗 Useful Commands

### Git Flow Init (if using git-flow extension)
```bash
git flow init
# Use defaults: master, develop, feature/, release/, hotfix/, v
```

### Quick Status
```bash
# See all branches
git branch -a

# See branch relationships
git log --oneline --graph --all

# Check branch protection
gh api repos/:owner/:repo/branches/master/protection
```

---

**Last Updated**: November 2025
**Version**: 1.0.0
**Maintainer**: @Ensyferum
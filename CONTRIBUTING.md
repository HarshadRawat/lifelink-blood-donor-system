# Contributing to LifeLink

Thank you for your interest in contributing to LifeLink! Please follow these guidelines to ensure code quality, security, and smooth collaboration.

---

## 1. Branch Strategy

Create feature branches from `main` using descriptive naming conventions:

* `feature/<short-description>`: New functionality or enhancement
* `bugfix/<issue-description>`: Bug fixes and security patches
* `docs/<topic>`: Documentation updates and guides
* `refactor/<module>`: Code restructuring without functional changes

Example:
```bash
git checkout -b feature/donor-eligibility-checklist
```

---

## 2. Development Workflow

1. **Pull Latest Changes**:
   ```bash
   git checkout main
   git pull origin main
   ```
2. **Make Small, Focused Commits**:
   Keep commits modular and provide clear, imperative commit messages:
   ```bash
   git commit -m "feat(matching): optimize haversine distance computation"
   ```
3. **Follow Code Conventions**:
   * **Backend**: Standard Java naming conventions, Spring Boot service-layer isolation, DTO pattern (never expose JPA entities directly in REST controllers), method security guards.
   * **Frontend**: Functional React components with hooks, CSS variable tokens, accessible interactive elements.
4. **Never Commit Secrets**:
   * Do not commit `.env`, private keys, passwords, or personal credentials.
   * Always verify that `.gitignore` prevents secrets from staging.

---

## 3. Testing Requirements

Before submitting a Pull Request, all automated tests must pass with zero failures:

### Backend Test Suite
```powershell
cd blood-donor-system
.\mvnw.cmd test
```
*Verify that all 132+ unit, security, and integration tests pass.*

### Frontend Production Build
```powershell
cd blood-donor-system/frontend
npm run build
```
*Verify that Vite builds without bundling or syntax errors.*

---

## 4. Pull Request Checklist

Before opening your PR, ensure:

- [ ] Branch is up to date with `main`.
- [ ] No hardcoded passwords, tokens, or environment-specific URLs are present.
- [ ] Unit and integration tests have been added or updated for new code.
- [ ] `.\mvnw.cmd test` passes completely.
- [ ] `npm run build` succeeds without errors.
- [ ] API documentation in `docs/API.md` is updated if endpoints were modified.
- [ ] Pull request description clearly explains the motivation, changes, and verification steps.

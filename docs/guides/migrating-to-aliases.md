# Migrating to the Alias System

This guide helps teams transition from ID-based references to the alias system in Scopes. Whether you're migrating from another tool or adopting aliases in an existing Scopes setup, this guide provides strategies and best practices.

## Migration Scenarios

### Scenario 1: New Scopes Installation
If you're starting fresh with Scopes, you're already using aliases! Every scope automatically gets a canonical alias.

### Scenario 2: Existing Scopes Without Custom Aliases
If you've been using Scopes but only with auto-generated aliases, this guide helps you add meaningful custom aliases.

### Scenario 3: Migrating from Another Tool
If you're moving from Jira, Asana, Trello, or another tool, this guide shows how to preserve your existing references.

## Pre-Migration Checklist

- [ ] Inventory existing naming conventions
- [ ] Document current ID mappings
- [ ] Plan team training
- [ ] Schedule migration window
- [ ] Prepare rollback strategy

## Step-by-Step Migration

### Step 1: Audit Current System

#### For Existing Scopes Users
```bash
# List all scopes with only canonical aliases
$ scopes list --all --format json | \
  jq '.[] | select(.aliases | length == 1) | {id: .id, title: .title, canonical: .aliases[0]}'

# Export to CSV for planning
$ scopes list --all --format csv > scopes-audit.csv
```

#### For External Tool Migration
Create a mapping document:
```csv
external_id,title,suggested_alias
PROJ-123,Authentication System,auth-system
PROJ-124,User Management,user-mgmt
TASK-456,Fix Login Bug,fix-login-bug
```

### Step 2: Define Naming Conventions

#### Establish Team Standards
```yaml
# naming-conventions.yml
projects:
  pattern: "{team}-{project}-{component}"
  examples:
    - fe-website-header
    - be-api-auth
    - qa-tests-integration

tasks:
  pattern: "{type}-{description}"
  examples:
    - bug-login-validation
    - feat-user-profile
    - chore-update-deps

sprints:
  pattern: "sprint-{number}"
  examples:
    - sprint-42
    - sprint-43
```

#### Document Prefixes
| Prefix | Meaning | Example |
|--------|---------|---------|
| `fe-` | Frontend | `fe-navbar` |
| `be-` | Backend | `be-api` |
| `qa-` | QA/Testing | `qa-regression` |
| `doc-` | Documentation | `doc-api-guide` |
| `infra-` | Infrastructure | `infra-deploy` |

### Step 3: Add Custom Aliases

#### Manual Addition
For small teams or selective migration:

```bash
# Add meaningful alias to existing scope
$ scopes alias add quiet-river-x7k auth-system

# Add multiple aliases
$ scopes alias add quiet-river-x7k PROJ-123  # Legacy reference
$ scopes alias add quiet-river-x7k auth      # Short name
$ scopes alias add quiet-river-x7k authentication-module  # Descriptive
```

#### Bulk Import
For large-scale migration:

1. Create alias mapping file:
```csv
# aliases.csv
canonical_alias,custom_alias,legacy_id
quiet-river-x7k,auth-system,PROJ-123
bold-mountain-a9z,user-mgmt,PROJ-124
swift-ocean-k3m,api-gateway,PROJ-125
```

2. Convert to import format:
```bash
#!/bin/bash
# convert-aliases.sh
while IFS=, read -r canonical custom legacy; do
  echo "$custom=$canonical"
  [ -n "$legacy" ] && echo "$legacy=$canonical"
done < aliases.csv > import-aliases.txt
```

3. Import aliases:
```bash
$ scopes alias import import-aliases.txt
✓ Imported 150 aliases successfully
```

### Step 4: Update Team Workflows

#### Git Integration
Align git branches with scope aliases:

```bash
# Create feature branch matching scope
$ git checkout -b feature/auth-system
$ scopes alias add auth-system feature/auth-system

# Commit message references
$ git commit -m "feat(auth-system): Implement JWT validation"
```

#### IDE Integration
Configure IDE task managers:

```json
// .vscode/tasks.json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Focus on current feature",
      "type": "shell",
      "command": "scopes focus ${input:scopeAlias}"
    }
  ],
  "inputs": [
    {
      "id": "scopeAlias",
      "type": "promptString",
      "description": "Scope alias"
    }
  ]
}
```

#### CI/CD Pipeline
Reference scopes in automation:

```yaml
# .github/workflows/deploy.yml
- name: Update scope status
  run: |
    SCOPE_ALIAS="${{ github.event.pull_request.head.ref }}"
    scopes aspect set "$SCOPE_ALIAS" status=deployed
```

### Step 5: Team Training

#### Quick Reference Card
Create and distribute:
```markdown
## Scope Alias Cheat Sheet

### Finding Scopes
- By exact alias: `scopes show auth-system`
- By prefix: `scopes show auth-s`
- Search: `scopes alias search auth`

### Common Aliases
- Current sprint: `sprint-42`
- Your feature: `feat-your-name`
- Hotfixes: `hotfix-description`

### Tips
- Tab completion works!
- Case doesn't matter
- Use prefixes for filtering
```

#### Training Sessions
1. **Basic Usage** (30 min)
   - Creating scopes with aliases
   - Finding scopes by alias
   - Adding custom aliases

2. **Advanced Workflows** (45 min)
   - Hierarchical naming
   - Context and focus with aliases
   - Integration with tools

3. **Best Practices** (30 min)
   - Naming conventions
   - When to use multiple aliases
   - Maintenance and cleanup

### Step 6: Gradual Rollout

#### Phase 1: Pilot Team (Week 1-2)
- Select early adopters
- Gather feedback
- Refine conventions

```bash
# Pilot team adds aliases to their scopes
$ scopes list --filter "team=alpha" | while read scope; do
  # Add team prefix
  scopes alias add "$scope" "alpha-$scope"
done
```

#### Phase 2: Department (Week 3-4)
- Expand to full department
- Document lessons learned
- Create templates

#### Phase 3: Organization (Week 5-6)
- Roll out company-wide
- Monitor adoption
- Support stragglers

### Step 7: Cleanup and Optimization

#### Remove Obsolete References
After successful migration:

```bash
# Find unused legacy aliases
$ scopes alias list --all | grep -E "^(PROJ|TASK|ISSUE)-[0-9]+"

# Remove if no longer needed
$ scopes alias rm PROJ-123
$ scopes alias rm TASK-456
```

#### Optimize Naming
Standardize inconsistent aliases:

```bash
# Find non-standard aliases
$ scopes alias list --all | grep -v -E "^(fe|be|qa|doc|infra)-"

# Rename to follow conventions
$ scopes alias add messy-scope-name be-api-auth
$ scopes alias rm messy-scope-name
```

## Migration Strategies by Tool

### From Jira
```bash
# Preserve Jira IDs as aliases
$ scopes create "Epic: Authentication" --alias EPIC-100
$ scopes create "Story: User login" --alias STORY-101 --parent EPIC-100

# Add meaningful aliases alongside
$ scopes alias add EPIC-100 auth-epic
$ scopes alias add STORY-101 user-login
```

### From GitHub Issues
```bash
# Use issue numbers
$ scopes create "Bug: Login fails on mobile" --alias issue-1234
$ scopes alias add issue-1234 mobile-login-bug

# Link PR references
$ scopes alias add issue-1234 pr-5678
```

### From Trello
```bash
# Preserve board/card structure
$ scopes create "Development Board" --alias dev-board
$ scopes create "In Progress: API Design" --alias card-api-design --parent dev-board

# Add status aliases
$ scopes alias add card-api-design in-progress
```

## Common Challenges and Solutions

### Challenge 1: Resistance to Change
**Problem**: Team members prefer old IDs

**Solution**:
- Keep legacy IDs as aliases during transition
- Show productivity gains with demos
- Make aliases even shorter than IDs

### Challenge 2: Naming Conflicts
**Problem**: Multiple teams want same alias

**Solution**:
```bash
# Use team prefixes
$ scopes alias add scope-1 teamA-auth
$ scopes alias add scope-2 teamB-auth

# Or use hierarchical naming
$ scopes alias add scope-1 platform-auth
$ scopes alias add scope-2 mobile-auth
```

### Challenge 3: Forgotten Aliases
**Problem**: Can't remember what alias was used

**Solution**:
```bash
# Search by partial match
$ scopes alias search auth

# Search in descriptions
$ scopes list --all | grep -i "authentication"

# Use consistent patterns
$ scopes alias add scope auth-2024-q1  # Include timeframe
```

## Rollback Plan

If migration causes issues:

1. **Keep Legacy IDs**: Don't remove old references immediately
2. **Export Mappings**: Always backup before bulk changes
   ```bash
   $ scopes alias export > backup-$(date +%Y%m%d).txt
   ```
3. **Gradual Adoption**: Allow both systems during transition
4. **Document Issues**: Track problems for resolution

## Success Metrics

Track adoption progress:

```bash
# Percentage of scopes with custom aliases
$ echo "scale=2; $(scopes alias list --custom | wc -l) / $(scopes list --all | wc -l) * 100" | bc

# Most used aliases (indicates adoption)
$ scopes alias stats --top 20

# Team adoption by prefix usage
$ scopes alias list --all | grep -c "^fe-"  # Frontend team adoption
```

## Best Practices for Sustainable Alias Usage

### DO:
- ✅ Document naming conventions
- ✅ Regular alias audits
- ✅ Include in onboarding
- ✅ Automate where possible
- ✅ Keep legacy references during transition

### DON'T:
- ❌ Force immediate adoption
- ❌ Remove old IDs too quickly
- ❌ Create overly complex patterns
- ❌ Ignore team feedback
- ❌ Skip training

## Next Steps

1. Review [Using Aliases Guide](./using-aliases.md) for ongoing usage
2. Set up [Team Workflows](./team-workflows.md) for collaboration
3. Configure [IDE Integration](./ide-integration.md) for development
4. Plan regular [Alias Maintenance](./alias-maintenance.md) reviews

## Conclusion

Successful migration to aliases improves team productivity and system usability. Take it gradually, involve your team, and maintain flexibility during the transition. The investment in meaningful names pays dividends in long-term maintainability and team collaboration.

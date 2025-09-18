# Working with Contexts Tutorial

This tutorial teaches you how to use context views to organize and filter your scopes for different work situations. Contexts are like saved searches or workspaces that help you focus on relevant tasks.

## What You'll Learn

- Understanding context views
- Creating and managing contexts
- Switching between work modes
- Combining contexts with other features

## Prerequisites

- Completed the [Getting Started](./getting-started.md) tutorial
- Some existing scopes to work with

## What are Contexts?

A context is a named filter that automatically applies when you work with scopes. Think of contexts as different "lenses" through which you view your tasks:

- **Work context**: Only show work-related tasks
- **Personal context**: Only show personal projects
- **Urgent context**: Only show high-priority items
- **Current sprint**: Only show tasks for the current iteration

## Step 1: Create Sample Scopes

Let's create a diverse set of scopes to demonstrate contexts:

```bash
# Work projects
scopes create "Backend API development" --alias backend-api
scopes create "Frontend redesign" --alias frontend
scopes create "Database optimization" --alias db-optimize

# Personal projects
scopes create "Learn photography" --alias photography
scopes create "Home renovation" --alias home-reno
scopes create "Fitness goals" --alias fitness

# Set aspects to differentiate them
scopes aspect set backend-api type=work priority=high
scopes aspect set frontend type=work priority=medium
scopes aspect set db-optimize type=work priority=low

scopes aspect set photography type=personal priority=low
scopes aspect set home-reno type=personal priority=high
scopes aspect set fitness type=personal priority=medium
```

## Step 2: Your First Context

Create a context for work tasks:

```bash
scopes context create work-tasks "Work Tasks" \
  --filter "type=work" \
  -d "All work-related scopes"
```

Output:
```
✓ Context view 'work-tasks' created successfully
Key: work-tasks
Name: Work Tasks
Description: All work-related scopes
Filter: type=work
```

## Step 3: Switch to the Context

Activate the work context:

```bash
scopes context switch work-tasks
```

Output:
```
✓ Switched to context 'work-tasks'
Active filter: type=work
```

Now list your scopes:

```bash
scopes list
```

Output:
```
Using context filter: type=work
Found 3 scopes:

backend-api         Backend API development       type=work priority=high
frontend           Frontend redesign             type=work priority=medium
db-optimize        Database optimization         type=work priority=low
```

Notice that only work-related scopes are shown!

## Step 4: Create Multiple Contexts

Let's create more contexts for different situations:

### Personal Context

```bash
scopes context create personal "Personal Projects" \
  --filter "type=personal" \
  -d "Personal and hobby projects"
```

### Urgent Context

```bash
scopes context create urgent "Urgent Items" \
  --filter "priority=high" \
  -d "High priority tasks across all projects"
```

### Today's Focus

```bash
# First, mark some tasks for today
scopes aspect set backend-api focus=today
scopes aspect set fitness focus=today

# Create context
scopes context create today "Today's Focus" \
  --filter "focus=today" \
  -d "Tasks to focus on today"
```

## Step 5: List and Explore Contexts

View all your contexts:

```bash
scopes context list
```

Output:
```
Context Views (4):

• work-tasks [CURRENT] - Work Tasks
  All work-related scopes
  Filter: type=work

• personal - Personal Projects
  Personal and hobby projects
  Filter: type=personal

• urgent - Urgent Items
  High priority tasks across all projects
  Filter: priority=high

• today - Today's Focus
  Tasks to focus on today
  Filter: focus=today
```

## Step 6: Switch Between Contexts

Switch to see different views of your tasks:

### View urgent items across all projects:

```bash
scopes context switch urgent
scopes list
```

Output:
```
Using context filter: priority=high
Found 2 scopes:

backend-api         Backend API development       type=work priority=high
home-reno          Home renovation               type=personal priority=high
```

### View today's focus:

```bash
scopes context switch today
scopes list
```

Output:
```
Using context filter: focus=today
Found 2 scopes:

backend-api         Backend API development       type=work priority=high focus=today
fitness            Fitness goals                 type=personal priority=medium focus=today
```

## Step 7: Clear Context (View Everything)

To see all scopes without filtering:

```bash
scopes context current --clear
```

Output:
```
✓ Current context cleared. All scopes will be visible.
```

Now `scopes list` shows everything:

```bash
scopes list
```

Output:
```
Found 6 scopes:

backend-api         Backend API development       type=work priority=high focus=today
frontend           Frontend redesign             type=work priority=medium
db-optimize        Database optimization         type=work priority=low
photography        Learn photography             type=personal priority=low
home-reno          Home renovation               type=personal priority=high
fitness            Fitness goals                 type=personal priority=medium focus=today
```

## Step 8: Override Context Temporarily

Sometimes you need to see all scopes while a context is active:

```bash
# Switch back to work context
scopes context switch work-tasks

# List with context
scopes list  # Shows only work items

# Override context temporarily
scopes list --no-context  # Shows all items
```

## Step 9: Complex Context Filters

Contexts support complex filter expressions:

### Create a context for current sprint work:

```bash
# Mark some work as current sprint
scopes aspect set backend-api sprint=current
scopes aspect set frontend sprint=current

# Create complex context
scopes context create sprint-work "Current Sprint Work" \
  --filter "type=work AND sprint=current" \
  -d "Work items in the current sprint"
```

### Create a context for neglected items:

```bash
# Add last-updated aspect to some scopes
scopes aspect set photography last-updated=2024-12-01
scopes aspect set db-optimize last-updated=2024-12-15

# Context for items not updated recently
scopes context create neglected "Neglected Items" \
  --filter "NOT (focus=today OR priority=high)" \
  -d "Items that might need attention"
```

## Step 10: Edit Existing Contexts

Contexts can be modified as your needs change:

```bash
# Update the work context to exclude completed items
scopes context edit work-tasks \
  --filter "type=work AND NOT status=completed" \
  --name "Active Work Tasks"
```

## Step 11: Context Workflow Example

Here's a typical daily workflow using contexts:

### Morning Planning

```bash
# 1. Switch to today's context
scopes context switch today

# 2. Review what's planned
scopes list

# 3. Add more items to today if needed
scopes aspect set frontend focus=today

# 4. Check urgent items haven't been missed
scopes context switch urgent
scopes list
```

### During Work Hours

```bash
# Focus on work tasks only
scopes context switch work-tasks
scopes list

# Work through tasks...
scopes aspect set backend-api status=in-progress
```

### End of Day Review

```bash
# 1. Clear context to see everything
scopes context current --clear

# 2. Review all tasks
scopes list

# 3. Update focus for tomorrow
scopes aspect remove backend-api focus
scopes aspect set db-optimize focus=tomorrow
```

## Step 12: Delete Unused Contexts

Remove contexts you no longer need:

```bash
scopes context delete neglected
```

Output:
```
✓ Context view 'neglected' deleted successfully
```

## Best Practices

### 1. Name Contexts Clearly
Use descriptive names that immediately convey the filter's purpose.

### 2. Document with Descriptions
Always add descriptions to remember why you created each context.

### 3. Keep Filters Simple
Complex filters are powerful but harder to understand later.

### 4. Regular Cleanup
Delete contexts that you haven't used in a while.

### 5. Combine with Aspects
Design your aspects to work well with contexts.

## Advanced Tips

### Context Hierarchies

Create contexts that progressively narrow focus:

```bash
# Broad context
scopes context create all-work "All Work" --filter "type=work"

# Narrower context
scopes context create active-work "Active Work" \
  --filter "type=work AND NOT status=completed"

# Very specific context
scopes context create urgent-active-work "Urgent Active Work" \
  --filter "type=work AND NOT status=completed AND priority=high"
```

### Project-Specific Contexts

Create contexts for individual projects:

```bash
scopes context create project-alpha "Project Alpha" \
  --filter "project=alpha OR parent=project-alpha-root"
```

### Time-Based Contexts

Use contexts for time management:

```bash
scopes context create this-week "This Week" \
  --filter "deadline>=2025-01-20 AND deadline<=2025-01-26"
```

## Troubleshooting

### Context Not Filtering

If your context doesn't seem to filter:
1. Check the current context: `scopes context current`
2. Verify the filter: `scopes context show <context-key>`
3. Check aspect values: `scopes get <scope-alias>`

### Complex Filters Not Working

Break down complex filters to debug:
```bash
# Instead of
--filter "type=work AND priority=high AND NOT status=done"

# Test each part
scopes list -a type=work
scopes list -a priority=high
scopes list -a status=done
```

## Summary

You've learned how to:
- ✅ Create context views with filters
- ✅ Switch between different work modes
- ✅ Combine multiple filter conditions
- ✅ Edit and manage contexts
- ✅ Use contexts in daily workflows
- ✅ Override contexts when needed

## Next Steps

- Explore [Focus Management](./focus-management.md) for even more precise control
- Learn about [Organizing with Hierarchies](./organizing-with-hierarchies.md)
- Read the [Context Command Reference](../reference/cli-quick-reference.md#context-management)

Contexts are powerful tools for managing attention and organizing work. Use them to create personalized workflows that match your working style!
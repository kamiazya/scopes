# Shell Completion Setup Guide

This guide explains how to set up shell completion for the Scopes CLI, including tag (aspect) completion.

## Generating Completion Scripts

The Scopes CLI uses Clikt's built-in completion support. You can generate completion scripts for different shells:

### Bash

```bash
# Generate bash completion script
scopes --generate-completion bash > ~/scopes-completion.bash

# Source it in your current session
source ~/scopes-completion.bash

# To make it permanent, add to your ~/.bashrc:
echo "source ~/scopes-completion.bash" >> ~/.bashrc
```

### Zsh

```bash
# Generate zsh completion script
scopes --generate-completion zsh > _scopes

# Add to your fpath (typically in ~/.zshrc)
fpath=(~/path/to/completion/dir $fpath)
autoload -U compinit
compinit

# Or directly add to zsh completion directory
sudo mv _scopes /usr/share/zsh/site-functions/
```

### Fish

```bash
# Generate fish completion script
scopes --generate-completion fish > scopes.fish

# Add to fish completion directory
mv scopes.fish ~/.config/fish/completions/
```

## Using Tag (Aspect) Completion

The `list` command supports filtering by aspects (tags) with shell completion:

```bash
# List scopes with specific aspects
# Both key:value and key=value are supported
scopes list --aspect priority:high
scopes list --aspect priority=high
scopes list -a status:active -a priority:high
scopes list -a status=active -a priority=high

# Tab completion will suggest available aspect:value pairs
scopes list --aspect <TAB>
```

### How It Works

1. When you type `scopes list --aspect <TAB>`, the shell completion script calls `scopes _complete-aspects`
2. This hidden command queries root scopes and their children for available aspect key:value pairs (also valid with key=value)
3. The results are presented as completion candidates

### Multiple Aspect Filters

You can specify multiple aspect filters (either key:value or key=value). A scope matches if it has ALL the specified aspects:

```bash
# Find scopes that have both "priority:high" AND "status:active"
scopes list --aspect priority:high --aspect status:active
# Or using '='
scopes list --aspect priority=high --aspect status=active

# Short form
scopes list -a priority:high -a status:active
```

## Troubleshooting

### Completion Not Working

1. Ensure the completion script is sourced:
   ```bash
   # Check if completion is loaded (bash)
   complete -p scopes
   ```

2. Regenerate the completion script after updates:
   ```bash
   scopes --generate-completion bash > scopes-completion.bash
   source scopes-completion.bash
   ```

3. Check if the `_complete-aspects` command works:
   ```bash
   scopes _complete-aspects
   # Should list available aspect:value pairs
   ```

### No Aspect Suggestions

If no aspect suggestions appear, it might be because:
- No scopes with aspects exist in the database
- Database connection issues
- The `_complete-aspects` command encounters an error (the command is silent by design; run it directly to verify output or check application logs)

## Implementation Details

The aspect completion feature consists of:

1. **ListCommand**: Accepts `--aspect` parameters with `CompletionCandidates.Custom`.
2. **CompletionCommand**: Hidden command that provides aspect candidates.
3. **Shell completion script**: Calls the hidden command to get suggestions.

This design allows dynamic completion based on actual database content rather than static lists.

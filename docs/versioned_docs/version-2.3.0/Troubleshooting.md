# Troubleshooting
If you're having troubles with getting the plugin to work follow the following steps:
:::info
this section is a work in progress and will be expanded over time
:::

## Is the plugin loaded?
Does the plugin show up in `/pl` and is it green?
If it’s not there, you might not have installed the plugin correctly or haven’t restarted the server yet.

## Is the config correct?
Make sure that your config is correct, and that the enabled field on your rules is set to true!

## Check your logs!
Check your console for errors, especially on server start and when a player dies. If there are any errors, they might give you a clue as to what is going wrong.
You should also skim trough the `latest.log` file located in the `logs` folder of your server for any errors related to BetterKeepInventory.

## See what your rules are doing
BetterKeepInventory can show you how it handled a death: which rules ran, whether their conditions passed, and which effects were applied.

### Watch it in chat
Run `/bki debug verbose` in-game, then die (or wait for someone else to). The rule evaluation is sent to your chat as it happens. Run the command again to turn it off.

This shows **every** death on the server, not just your own, and turns itself off when the server restarts.

### Show it in the console
Set `debug: true` in your `config.yml` and run `/bki reload`. Every death now prints its rule evaluation to the console, along with per-item details like why an item was or wasn't dropped. Turn it back off when you're done, it gets noisy on busy servers.

### Create a debug report
Run `/bki debug upload` (works from the console too). This uploads a report and gives you a link to share. A copy is also saved as `plugins/BetterKeepInventory/debugger.log`. The report contains:
- Your server software, Minecraft, Java and plugin versions
- Your `config.yml`
- The rule evaluations of the most recent deaths (the last 1000 lines)

Deaths are recorded for the report even when `debug` is off, so you can create one right after something went wrong.

:::tip
Asking for help? Reproduce the problem, run `/bki debug upload` and share the link in your post.
:::

## Still having issues?

Feel free to join the [Discord](https://beepsterr.com/discord) for help! Just `#create-a-ticket` and i’ll help you out!
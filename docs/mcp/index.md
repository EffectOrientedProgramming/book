---
hide:
- navigation
---
# MCP Book

The *Effect Oriented Programming* book is available as an [MCP](https://modelcontextprotocol.io/) (Model Context Protocol) server, enabling you to use AI assistants to explore and interact with the book's content directly within your development environment.

## What Is the MCP Book Server?

The MCP server at `https://mcp.effectorientedprogramming.com` exposes the book and related resources as MCP tools and resources. Each chapter is available as an MCP resource, allowing AI assistants to read, search, and reference the book's content when helping you write code or answer questions about Effect-Oriented Programming.

This means you can ask your AI assistant questions about the book, get code examples, and reference specific chapters — all without leaving your editor or chat interface.

## Authentication

The MCP server requires authentication using the **email address you used to purchase the book**.

## Setup Instructions

### claude.ai

1. Go to [claude.ai/settings/connectors](https://claude.ai/settings/connectors)
2. Click **"Add custom connector"**
3. Enter a name for the connector, like "EOP Book" and the server URL: `https://mcp.effectorientedprogramming.com`
4. Click **"Add"**
5. Locate the new connector and click **"Connect"** to initiate the authentication flow
6. Complete the authentication flow in the browser

In a chat, click the **"+"** button, select **"Connectors"**, and toggle the connector on.

### Claude Desktop

1. Open Claude Desktop and go to **Settings > Connectors**
2. Click **"Add custom connector"**
3. Enter the server URL: `https://mcp.effectorientedprogramming.com`
4. Click **"Add"**
5. Complete the authentication flow in the browser

### Claude Code (CLI)

Run the following command:

```
claude mcp add --transport http eop-book https://mcp.effectorientedprogramming.com
```

Use the `/mcp` command inside Claude Code to complete the authentication flow.

### Cursor

Add the following to your `.cursor/mcp.json` file (project-level) or `~/.cursor/mcp.json` (global):

```json
{
  "mcpServers": {
    "eop-book": {
      "url": "https://mcp.effectorientedprogramming.com"
    }
  }
}
```

### VS Code / GitHub Copilot

Add the following to your `.vscode/mcp.json` file:

```json
{
  "servers": {
    "eop-book": {
      "type": "http",
      "url": "https://mcp.effectorientedprogramming.com"
    }
  }
}
```

### ChatGPT

1. Open [ChatGPT](https://chat.openai.com) and go to **Settings > Apps > Advanced settings**
2. Toggle **Developer Mode** on
3. Go to **Settings > Connectors** and click **Create**
4. Enter:
    - **Name:** `Effect Oriented Programming`
    - **Connector URL:** `https://mcp.effectorientedprogramming.com/`
5. Click **Create** and complete the authentication flow

To use in a chat, click **+** near the message composer, select **More > Developer Mode**, and activate the connector.

Available on Pro, Plus, Business, Enterprise, and Education plans.

### Kiro

Add the following to your Kiro MCP configuration file:

- **User-level (global):** `~/.kiro/settings/mcp.json`
- **Workspace-level:** `<project-root>/.kiro/settings/mcp.json`

```json
{
  "mcpServers": {
    "eop-book": {
      "url": "https://mcp.effectorientedprogramming.com"
    }
  }
}
```

### Windsurf

Add the following to your Windsurf MCP configuration file (`~/.codeium/windsurf/mcp_config.json`):

```json
{
  "mcpServers": {
    "eop-book": {
      "serverUrl": "https://mcp.effectorientedprogramming.com"
    }
  }
}
```

Restart Windsurf after saving.

### Other MCP-Compatible Clients

Any MCP-compatible client that supports streamable HTTP transport can connect to the book server using the URL:

```
https://mcp.effectorientedprogramming.com
```

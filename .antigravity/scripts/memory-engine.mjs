#!/usr/bin/env node
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

const command = process.argv[2] || 'help';
const query = process.argv.slice(3).join(' ');

function getAllFiles(dirPath, arrayOfFiles = []) {
  if (!fs.existsSync(dirPath)) return arrayOfFiles;
  const files = fs.readdirSync(dirPath);

  files.forEach(file => {
    const fullPath = path.join(dirPath, file);
    if (fs.statSync(fullPath).isDirectory()) {
      arrayOfFiles = getAllFiles(fullPath, arrayOfFiles);
    } else if (file.endsWith('.md') || file.endsWith('.json')) {
      arrayOfFiles.push(fullPath);
    }
  });

  return arrayOfFiles;
}

switch (command) {
  case 'status': {
    console.log('=== Antigravity Global Memory Engine Status ===');
    const allFiles = getAllFiles(rootDir);
    console.log(`Total Knowledge & Memory Documents: ${allFiles.length}`);
    const indexes = getAllFiles(path.join(rootDir, 'indexes'));
    console.log(`Indexes: ${indexes.map(f => path.basename(f)).join(', ')}`);
    const projects = fs.existsSync(path.join(rootDir, 'projects')) 
      ? fs.readdirSync(path.join(rootDir, 'projects')).filter(f => fs.statSync(path.join(rootDir, 'projects', f)).isDirectory())
      : [];
    console.log(`Registered Projects: ${projects.join(', ')}`);
    break;
  }

  case 'search': {
    if (!query) {
      console.log('Usage: node memory-engine.mjs search <query>');
      process.exit(1);
    }
    console.log(`Searching Antigravity Memory for: "${query}"...\n`);
    const files = getAllFiles(rootDir);
    const matches = [];

    const lowerQuery = query.toLowerCase();
    for (const file of files) {
      try {
        const content = fs.readFileSync(file, 'utf8');
        if (content.toLowerCase().includes(lowerQuery)) {
          const relPath = path.relative(rootDir, file).replace(/\\/g, '/');
          const lines = content.split('\n');
          const matchedLines = lines
            .map((line, idx) => ({ line, num: idx + 1 }))
            .filter(item => item.line.toLowerCase().includes(lowerQuery))
            .slice(0, 3);

          matches.push({ relPath, matchedLines });
        }
      } catch (err) {
        // ignore read errors
      }
    }

    if (matches.length === 0) {
      console.log('No matches found in Antigravity Memory.');
    } else {
      console.log(`Found ${matches.length} matching file(s):\n`);
      for (const m of matches) {
        console.log(`File: .antigravity/${m.relPath}`);
        for (const item of m.matchedLines) {
          console.log(`  Line ${item.num}: ${item.line.trim()}`);
        }
        console.log('');
      }
    }
    break;
  }

  case 'context': {
    const contextFile = path.join(rootDir, 'projects', 'scanflow', 'project-context.md');
    if (fs.existsSync(contextFile)) {
      console.log(fs.readFileSync(contextFile, 'utf8'));
    } else {
      console.log('No project context found for ScanFlow.');
    }
    break;
  }

  case 'help':
  default: {
    console.log('Antigravity Global Memory Engine CLI');
    console.log('Commands:');
    console.log('  node memory-engine.mjs status          Check memory system health');
    console.log('  node memory-engine.mjs search <query>  Search all memory files');
    console.log('  node memory-engine.mjs context         Display current project context');
    break;
  }
}

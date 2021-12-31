#!/usr/bin/env node
/**
 * Update the NpmPackage annotation for all modules in the project
 * by checking versions published in npm repository.
 * Example
 *   ./scripts/updateNpmVer.js
 */

const fs = require('fs');
const util = require('util');
const xml2js = require('xml2js');
const exec = util.promisify(require('child_process').exec);
const replace = require('replace-in-file');
const cachedNpmVersions = {};

async function getAnnotations(){
  const cmd = 'grep -r @NpmPackage ./*/src/*/java';
  const output = await run(cmd);
  const lines = output.split('\n').filter(Boolean);
  let modules = [];
  return lines.map(line => {
    const r = /(collaboration-engin.*):(.*value *= *"([^"]+).*version *= *"((\d+)\.(\d+)[^"]*).*)/.exec(line);
    if (!r){
      const errorPackage = /(collaboration-engin.*)/.exec(line); 
      console.log(`versions.js::getAnnotations : cannot get the annotation properly for ${errorPackage[2]} in ${errorPackage[1]}`);
      process.exit(1);
    }
    return {
      path: r[1],
      annotation: r[2],
      package: r[3],
      version: r[4],
      major: r[5],
      minor: r[6],
      updatedVersion: ''
    };
  });
}

async function getLatestNpmVersion(package, version, major, minor) {
  if (!cachedNpmVersions[package]) {
    cmd = `npm view ${package} versions --json`;
    const json = await JSON.parse(await run(cmd))
    const versions = json
       .filter(version => version.startsWith(`${major}.${minor}`))
       .map(a => a.replace(/\d+$/, n => +n+900000))
       .sort()
       .map(a => a.replace(/\d+$/, n => +n-900000));
    const next =  versions.pop();
    console.log(`Checking next Npm version for ${package} ${version} ${next}`);
    cachedNpmVersions[package] = next;
  }
  return cachedNpmVersions[package];
}

async function computeVersionToUpdate(data) {
  return (data['updatedVersion'] = await getLatestNpmVersion(data.package, data.version, data.major, data.minor));
}

async function updateFiles(moduleData){
  if(moduleData.annotation.length>0){
    if (moduleData.version != moduleData.updatedVersion){
      updatedNpm = moduleData.annotation.replace(moduleData.version, moduleData.updatedVersion)
      let options = {
        files: moduleData.path,
        from: moduleData.annotation,
        to: updatedNpm,
      };
      try {
        const results = await replace(options)
        console.log('\x1b[33m', "Updated "+ moduleData.package + " from version " +
                    moduleData.version + " to " + moduleData.updatedVersion);
      }
      catch (error) {
        console.error('Error occurred:', error);
      }
    } else {
      console.log('\x1b[32m', "No need to update annotation for package " + moduleData.package +
                  ", as version " + moduleData.version + " is the latest");
    }
  }
}

async function run(cmd) {
  const { stdout, stderr } = await exec(cmd);
  return stdout;
}

async function main() {
  console.log("Updating the NpmPackage annotation.")
  const annotations = await getAnnotations();
  
  for (i = 0; i < annotations.length; i++) {
    await computeVersionToUpdate(annotations[i]);
    await updateFiles(annotations[i]);
  }
}

main();

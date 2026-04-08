# Key rotation checklist

## Purpose

Use this checklist when rotating signing or SSH-related keys.

## Keep track of

- Which keys were rotated
- Where the replacement key lives in `~/dev/creds/`
- Which environments need the new value
- Whether any old material still exists in local caches or build output

## Do not do

- Put private material directly into the repository
- Treat this checklist as a secret store

---
version: alpha
name: Termex-Android
description: Compose SSH client with neutral Material shell, terminal-green brand cues, and configurable command palettes for long mobile sessions.
colors:
  background: "#191C1A"
  background-alt: "#1E1E1E"
  surface: "#1E1E1E"
  surface-light: "#FBFDF8"
  primary: "#00D084"
  secondary: "#B8F5D8"
  tertiary: "#A8EEF7"
  text: "#E1E3DF"
  text-muted: "#B0B6B1"
  success: "#00D084"
  warning: "#FFC463"
  danger: "#CD0000"
typography:
  display-lg:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "32px"
    fontWeight: 600
    lineHeight: "40px"
    letterSpacing: "0em"
  headline-md:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: "32px"
    letterSpacing: "0em"
  body-md:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0.03em"
  label-sm:
    fontFamily: "Roboto, system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "16px"
    letterSpacing: "0.04em"
  mono-sm:
    fontFamily: "monospace"
    fontSize: "13px"
    fontWeight: 500
    lineHeight: "18px"
    letterSpacing: "0em"
rounded:
  sm: "12px"
  md: "14px"
  lg: "18px"
  xl: "24px"
  full: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#003823"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
    height: "48px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    typography: "{typography.body-md}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
  input-field:
    backgroundColor: "{colors.background-alt}"
    textColor: "{colors.text}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
  terminal-pane:
    backgroundColor: "#000000"
    textColor: "#E5E5E5"
    typography: "{typography.mono-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
---

## Overview
Termex-Android should feel like a serious mobile SSH tool with Android-native structure. The main shell uses Material rhythm and accessibility, while terminal surfaces carry the product's stronger personality.

## Colors
Terminal green is the anchor color for the Android shell. Dark mode is the natural default. Terminal sessions can shift into black, phosphor green, amber, ocean, nord, solarized, paper, sepia, and forest variants.

## Typography
Use standard Material type for app structure and forms. Terminal content, key bars, and technical details should use monospaced text and strong contrast.

## Layout
Keep the outer app in clear stacked sections with large tap targets. Terminal and multi-terminal views can become denser, but should still preserve obvious control zones and reading order.

## Elevation & Depth
Use Material tonal elevation for the shell. Terminal panes should feel self-contained and slightly heavier than management cards.

## Shapes
Use medium-rounded cards and inputs across the app. Compact controls, chips, and terminal overlays can tighten slightly, but the overall geometry should stay soft and stable.

## Components
Core pieces are onboarding cards, server and key management lists, terminal panes, extra key bars, local transfer or diagnostics surfaces, and security-aware action areas.

## Do's and Don'ts
- Do keep terminal readability above aesthetic novelty.
- Do let the shell stay touch-friendly even when the data density rises.
- Don't make every screen look like a full-screen terminal.
- Don't use bright terminal colors as background decoration outside the terminal itself.

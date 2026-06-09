#!/usr/bin/env python3
"""
Visualize an XDF recording — handles single-channel and multi-channel streams.

One subplot per channel, all sharing a time axis.  High-rate waveforms appear
on top with taller subplots; low-rate biometrics follow below.
Multi-channel streams are kept together and labelled "StreamName_ChannelLabel".

Requirements:
    pip install pyxdf matplotlib numpy

Usage:
    python visualize_xdf.py recording.xdf
    python visualize_xdf.py recording.xdf --start 10 --end 60
"""

import sys
import argparse
import numpy as np
import matplotlib.pyplot as plt
import pyxdf


WAVEFORM_HZ_THRESHOLD = 10   # nominal rate at or above this → waveform subplot height


# ── helpers ──────────────────────────────────────────────────────────────────

def get_channel_labels(stream):
    """Return a list of channel label strings from LSL stream metadata."""
    n_ch = int(stream["info"]["channel_count"][0])
    labels = []
    try:
        channels = stream["info"]["desc"][0]["channels"][0]["channel"]
        for ch in channels:
            label = ch.get("label", [None])[0]
            labels.append(label if label else f"ch{len(labels)}")
    except (KeyError, IndexError, TypeError):
        pass
    # Pad with defaults if metadata is absent or incomplete
    while len(labels) < n_ch:
        labels.append(f"ch{len(labels)}")
    return labels[:n_ch]


def build_tracks(streams):
    """
    Flatten all streams into a list of track dicts, one per channel.

    Track fields:
        label        – display label for the y-axis
        ts           – 1-D array of timestamps (absolute LSL time)
        vals         – 1-D array of sample values
        fs_nom       – nominal sample rate
        is_waveform  – True when fs_nom >= WAVEFORM_HZ_THRESHOLD
        color        – matplotlib color string
    """
    # Assign one base color per stream from tab10
    try:
        palette = plt.colormaps["tab10"].colors      # matplotlib >= 3.5
    except AttributeError:
        palette = plt.get_cmap("tab10").colors       # matplotlib < 3.5
    tracks = []

    for stream_idx, stream in enumerate(streams):
        name    = stream["info"]["name"][0]
        stype   = stream["info"]["type"][0]
        fs_nom  = float(stream["info"]["nominal_srate"][0])
        n_ch    = int(stream["info"]["channel_count"][0])
        labels  = get_channel_labels(stream)
        color   = palette[stream_idx % len(palette)]

        for ch_idx in range(n_ch):
            # Single-channel streams: use the stream type as label (e.g. "ECG").
            # Multi-channel streams: "StreamName_ChannelLabel" (e.g. "BIOPAC_NIBP100E_HD_SBP").
            if n_ch == 1:
                display_label = stype if stype else name
            else:
                display_label = f"{name}_{labels[ch_idx]}"

            tracks.append({
                "label":       display_label,
                "ts":          stream["time_stamps"],
                "vals":        stream["time_series"][:, ch_idx],
                "fs_nom":      fs_nom,
                "is_waveform": fs_nom >= WAVEFORM_HZ_THRESHOLD,
                "color":       color,
            })

    return tracks


def sort_streams_by_rate(streams):
    """Sort streams highest-rate first; multi-channel streams stay together."""
    return sorted(streams, key=lambda s: float(s["info"]["nominal_srate"][0]), reverse=True)


# ── main plot ─────────────────────────────────────────────────────────────────

def plot_xdf(path, start_sec=None, end_sec=None):
    raw_streams, _ = pyxdf.load_xdf(path)
    if not raw_streams:
        sys.exit(f"No streams found in {path}")

    streams = sort_streams_by_rate(raw_streams)
    tracks  = build_tracks(streams)

    if not tracks:
        sys.exit("No plottable channels found.")

    # Height ratios: one entry per track
    heights = [3.0 if t["is_waveform"] else 1.5 for t in tracks]
    n = len(tracks)

    fig, axes = plt.subplots(
        n, 1,
        figsize=(18, min(sum(heights) + 1, 60)),   # cap at 60 in so it doesn't explode
        gridspec_kw={"height_ratios": heights},
        sharex=True,
    )
    if n == 1:
        axes = [axes]

    # Global t=0: earliest timestamp across all streams
    t0 = min(t["ts"][0] for t in tracks if len(t["ts"]) > 0)

    for ax, track in zip(axes, tracks):
        ts   = track["ts"] - t0
        vals = track["vals"]

        # Time-range crop
        lo = start_sec if start_sec is not None else ts[0]
        hi = end_sec   if end_sec   is not None else ts[-1]
        mask = (ts >= lo) & (ts <= hi)
        ts, vals = ts[mask], vals[mask]

        if len(ts) < 2:
            ax.text(0.5, 0.5, f"{track['label']}\n(no data in range)",
                    ha="center", va="center", transform=ax.transAxes, fontsize=8)
            ax.set_yticks([])
            continue

        duration_s = ts[-1] - ts[0]
        actual_fs  = len(ts) / duration_s if duration_s > 0 else track["fs_nom"]

        lw     = 0.5  if track["is_waveform"] else 1.0
        marker = None if track["is_waveform"] else "o"
        ms     = 0    if track["is_waveform"] else 3

        ax.plot(ts, vals, lw=lw, marker=marker, markersize=ms, color=track["color"])

        ax.set_ylabel(track["label"], fontsize=9, rotation=0,
                      ha="right", va="center", labelpad=6)

        rate_note = (
            f"{actual_fs:.2f} Hz  (nominal {track['fs_nom']:.2f} Hz)"
            f"  —  {len(vals):,} samples"
        )
        ax.text(0.995, 0.97, rate_note, transform=ax.transAxes,
                fontsize=7, va="top", ha="right", color="gray")

        ax.tick_params(axis="y", labelsize=7)
        ax.grid(True, alpha=0.25, linestyle="--")
        ax.margins(x=0.005)

    axes[-1].set_xlabel("Time (s)", fontsize=10)
    fig.suptitle(path, fontsize=8, y=1.001)
    plt.tight_layout()
    plt.show()


# ── entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Visualize an XDF recording (single- and multi-channel streams)"
    )
    parser.add_argument("xdf_file", help="Path to .xdf file")
    parser.add_argument("--start", type=float, default=None,
                        help="Crop start in seconds from t=0")
    parser.add_argument("--end",   type=float, default=None,
                        help="Crop end in seconds from t=0")
    args = parser.parse_args()
    plot_xdf(args.xdf_file, args.start, args.end)

<template>
  <div v-if="blockManager && blockState" class="timelapse-container" :class="{'open': blockState.timelapseOpen}">
    <!-- Floating Collapsed Pill (when closed) -->
    <div v-if="!blockState.timelapseOpen" class="collapsed-container">
      <button class="timelapse-toggle-btn" @click="toggleOpen" title="타임랩스 / 실시간 타임라인 열기">
        <span class="icon">⏱</span>
        <span class="label">타임랩스</span>
        <span v-if="blockState.renderedBlocks > 0" class="badge">
          {{ blockState.renderedBlocks }}
        </span>
        <span class="live-dot-mini" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
      </button>

      <button class="quick-sync-btn" :class="{'syncing': blockState.isSyncing}" @click.stop="syncMap" title="맵 및 블록 즉시 동기화 (새로고침 없이 갱신)">
        <span class="sync-icon" :class="{'spinning': blockState.isSyncing}">🔄</span>
      </button>
    </div>

    <!-- Main Expanded Control Panel -->
    <div v-else class="timelapse-panel">
      <!-- Top info & actions bar -->
      <div class="panel-header">
        <div class="header-left">
          <span class="icon">⏱</span>
          <span class="title">타임랩스</span>
          <span class="badge">{{ blockState.renderedBlocks }} / {{ blockState.totalEvents }}</span>
          <span v-if="blockState.syncStatusText" class="sync-status-badge">{{ blockState.syncStatusText }}</span>
        </div>

        <div class="header-right">
          <button class="sync-btn" :class="{'syncing': blockState.isSyncing}" @click="syncMap" title="맵 및 블록 즉시 동기화">
            <span class="sync-icon" :class="{'spinning': blockState.isSyncing}">🔄</span>
            <span class="sync-text">동기화</span>
          </button>

          <div class="status-indicator">
            <span class="live-dot" :class="{'pulsing': blockState.isPlaying || blockState.progress >= 0.99}"></span>
            <span class="status-text">
              {{ blockState.isPlaying ? '재생' : (blockState.progress >= 0.99 ? '실시간' : '일시정지') }}
            </span>
          </div>

          <button class="close-btn" @click="toggleOpen" title="최소화">✕</button>
        </div>
      </div>

      <!-- Recent player placement alert if available -->
      <div v-if="blockState.latestPlayer" class="recent-player-bar">
        <span class="player-tag">최근 설치:</span>
        <span class="player-name">{{ blockState.latestPlayer }}</span>
        <span class="block-name">{{ blockState.latestBlock }}</span>
      </div>

      <!-- Scrubber Timeline Slider -->
      <div class="slider-row">
        <span class="time-label start" title="서버 시작">00:00</span>
        <div class="slider-wrapper">
          <input
              type="range"
              min="0"
              max="1"
              step="0.001"
              :value="blockState.progress"
              @input="onSeek"
              class="timeline-slider"
              title="시간순 스크롤 / 탐색"
          />
          <div class="slider-fill" :style="{width: (blockState.progress * 100) + '%'}"></div>
        </div>
        <span class="time-label current" title="현재 시점">
          {{ formatTime(blockState.currentTime - blockState.serverStartTime) }}
        </span>
      </div>

      <!-- Control Buttons (Responsive Grid for Mobile) -->
      <div class="controls-row">
        <div class="main-action-group">
          <button class="ctrl-btn jump-btn" @click="jumpStart" title="처음으로">⏮</button>
          <button class="ctrl-btn play-btn" @click="togglePlay" :title="blockState.isPlaying ? '일시정지' : '재생'">
            {{ blockState.isPlaying ? '⏸ 정지' : '▶ 재생' }}
          </button>
          <button class="ctrl-btn live-btn" :class="{'active': blockState.progress >= 0.99 && !blockState.isPlaying}" @click="jumpEnd" title="실시간 시점으로">
            <span class="live-dot-inline"></span> 실시간
          </button>
          <button
              class="ctrl-btn follow-btn"
              :class="{'active': blockState.autoFollow}"
              @click="toggleFollow"
              title="설치 위치 자동 시점 추적"
          >
            🎯 추적
          </button>
        </div>

        <!-- Speed options -->
        <div class="speed-group" title="재생 배속">
          <button
              v-for="s in [1, 2, 5, 10, 25]"
              :key="s"
              class="speed-btn"
              :class="{'active': blockState.speed === s}"
              @click="setSpeed(s)"
          >
            {{ s }}x
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "TimelapseBar",
  computed: {
    appState() {
      return this.$bluemap ? this.$bluemap.appState : null;
    },
    blockManager() {
      return this.appState ? this.appState.blockManager : (this.$bluemap ? this.$bluemap.blockManager : null);
    },
    blockState() {
      return this.appState && this.appState.blockState ? this.appState.blockState : (this.blockManager ? this.blockManager.data : null);
    }
  },
  methods: {
    toggleOpen() {
      if (this.blockManager) {
        this.blockManager.toggleTimelapse();
      }
    },
    togglePlay() {
      if (this.blockManager) {
        this.blockManager.togglePlay();
      }
    },
    syncMap() {
      if (this.blockManager) {
        this.blockManager.syncMap();
      }
    },
    onSeek(e) {
      if (this.blockManager) {
        this.blockManager.seek(parseFloat(e.target.value));
      }
    },
    setSpeed(s) {
      if (this.blockManager) {
        this.blockManager.setSpeed(s);
      }
    },
    jumpStart() {
      if (this.blockManager) {
        this.blockManager.seek(0.0);
      }
    },
    jumpEnd() {
      if (this.blockManager) {
        this.blockManager.seek(1.0);
        this.blockManager.pause();
      }
    },
    toggleFollow() {
      if (this.blockState) {
        this.blockState.autoFollow = !this.blockState.autoFollow;
      }
    },
    formatTime(ms) {
      if (!ms || ms < 0) ms = 0;
      let totalSec = Math.floor(ms / 1000);
      let hours = Math.floor(totalSec / 3600);
      let minutes = Math.floor((totalSec % 3600) / 60);
      let seconds = totalSec % 60;

      if (hours > 0) {
        return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
      }
      return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.timelapse-container {
  position: absolute;
  bottom: calc(14px + env(safe-area-inset-bottom, 0px));
  left: 50%;
  transform: translateX(-50%);
  z-index: 10001;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
  font-family: inherit;
  width: calc(100% - 24px);
  max-width: 540px;
  touch-action: manipulation;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  .collapsed-container {
    display: flex;
    align-items: center;
    gap: 8px;

    .timelapse-toggle-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(16, 20, 30, 0.92);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);
      color: #f0f0f0;
      border: 1px solid rgba(255, 255, 255, 0.22);
      border-radius: 28px;
      padding: 10px 18px;
      font-size: 0.92rem;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 6px 22px rgba(0, 0, 0, 0.5);
      touch-action: manipulation;
      transition: all 0.2s ease;

      &:hover, &:active {
        background: rgba(28, 34, 48, 0.98);
        border-color: #00e5ff;
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0, 229, 255, 0.25);
      }

      .icon {
        font-size: 1.1rem;
      }

      .badge {
        background: #00e5ff;
        color: #000;
        font-size: 0.75rem;
        font-weight: 700;
        padding: 2px 7px;
        border-radius: 10px;
      }

      .live-dot-mini {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #4caf50;

        &.pulsing {
          background: #00e5ff;
          box-shadow: 0 0 8px #00e5ff;
          animation: pulse 1.5s infinite;
        }
      }
    }

    .quick-sync-btn {
      background: rgba(16, 20, 30, 0.92);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);
      color: #00e5ff;
      border: 1px solid rgba(0, 229, 255, 0.35);
      border-radius: 50%;
      width: 42px;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.15rem;
      cursor: pointer;
      box-shadow: 0 6px 22px rgba(0, 0, 0, 0.5);
      touch-action: manipulation;
      transition: all 0.2s ease;

      &:hover, &:active {
        background: rgba(0, 229, 255, 0.2);
        border-color: #00e5ff;
        transform: scale(1.06);
      }

      &.syncing {
        border-color: #ffb74d;
        color: #ffb74d;
      }
    }
  }

  .timelapse-panel {
    width: 100%;
    box-sizing: border-box;
    background: rgba(14, 18, 28, 0.94);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.16);
    border-radius: 18px;
    padding: 12px 14px;
    box-shadow: 0 10px 36px rgba(0, 0, 0, 0.65);
    display: flex;
    flex-direction: column;
    gap: 8px;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 6px;

        .icon {
          font-size: 1.05rem;
        }

        .title {
          font-weight: 700;
          font-size: 0.92rem;
          color: #fff;
        }

        .badge {
          background: rgba(0, 229, 255, 0.15);
          border: 1px solid rgba(0, 229, 255, 0.35);
          color: #00e5ff;
          font-size: 0.72rem;
          font-weight: 700;
          padding: 2px 7px;
          border-radius: 8px;
        }

        .sync-status-badge {
          background: rgba(76, 175, 80, 0.2);
          border: 1px solid rgba(76, 175, 80, 0.5);
          color: #81c784;
          font-size: 0.72rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 6px;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 8px;

        .sync-btn {
          background: rgba(0, 229, 255, 0.12);
          border: 1px solid rgba(0, 229, 255, 0.3);
          color: #00e5ff;
          padding: 4px 9px;
          border-radius: 12px;
          font-size: 0.78rem;
          font-weight: 700;
          display: flex;
          align-items: center;
          gap: 4px;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.15s ease;

          &:hover, &:active {
            background: rgba(0, 229, 255, 0.25);
            border-color: #00e5ff;
          }

          &.syncing {
            color: #ffb74d;
            border-color: #ffb74d;
            background: rgba(255, 183, 77, 0.15);
          }
        }

        .status-indicator {
          display: flex;
          align-items: center;
          gap: 5px;

          .live-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #4caf50;

            &.pulsing {
              background: #ff5252;
              box-shadow: 0 0 8px #ff5252;
              animation: pulse 1.5s infinite;
            }
          }

          .status-text {
            font-size: 0.75rem;
            font-weight: 600;
            color: #bbb;
          }
        }

        .close-btn {
          background: rgba(255, 255, 255, 0.1);
          border: none;
          color: #aaa;
          font-size: 0.82rem;
          border-radius: 50%;
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.15s ease;

          &:hover, &:active {
            background: rgba(255, 255, 255, 0.25);
            color: #fff;
          }
        }
      }
    }

    .recent-player-bar {
      font-size: 0.75rem;
      color: #aaa;
      display: flex;
      gap: 6px;
      align-items: center;
      background: rgba(255, 255, 255, 0.04);
      padding: 3px 8px;
      border-radius: 6px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      .player-tag {
        color: #888;
      }
      .player-name {
        color: #ffd54f;
        font-weight: 600;
      }
      .block-name {
        color: #00e5ff;
        font-family: monospace;
      }
    }

    .slider-row {
      display: flex;
      align-items: center;
      gap: 8px;

      .time-label {
        font-size: 0.78rem;
        font-variant-numeric: tabular-nums;
        font-weight: 600;
        color: #888;
        min-width: 40px;

        &.current {
          color: #00e5ff;
          text-align: right;
        }
      }

      .slider-wrapper {
        position: relative;
        flex: 1;
        display: flex;
        align-items: center;

        .slider-fill {
          position: absolute;
          left: 0;
          height: 6px;
          background: linear-gradient(90deg, #0091ea, #00e5ff);
          border-radius: 3px;
          pointer-events: none;
          z-index: 1;
        }

        .timeline-slider {
          position: relative;
          z-index: 2;
          width: 100%;
          height: 6px;
          -webkit-appearance: none;
          appearance: none;
          background: rgba(255, 255, 255, 0.15);
          border-radius: 3px;
          outline: none;
          cursor: pointer;
          margin: 0;

          &::-webkit-slider-thumb {
            -webkit-appearance: none;
            appearance: none;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: #ffffff;
            border: 2px solid #00e5ff;
            box-shadow: 0 0 10px rgba(0, 229, 255, 0.8);
            cursor: pointer;
            transition: transform 0.1s ease;

            &:active {
              transform: scale(1.3);
            }
          }
        }
      }
    }

    .controls-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 6px;

      .main-action-group {
        display: flex;
        align-items: center;
        gap: 5px;
      }

      .ctrl-btn {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.14);
        color: #eee;
        border-radius: 8px;
        padding: 6px 11px;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        touch-action: manipulation;
        transition: all 0.15s ease;

        &:hover, &:active {
          background: rgba(255, 255, 255, 0.18);
        }

        &.jump-btn {
          padding: 6px 9px;
          font-size: 0.9rem;
        }

        &.play-btn {
          background: #00e5ff;
          color: #000;
          font-weight: 700;
          padding: 6px 14px;

          &:hover, &:active {
            background: #26c6da;
          }
        }

        &.live-btn {
          background: rgba(244, 67, 54, 0.15);
          border-color: rgba(244, 67, 54, 0.4);
          color: #ff5252;

          .live-dot-inline {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #ff5252;
          }

          &.active {
            background: #ff5252;
            color: #fff;
            .live-dot-inline {
              background: #fff;
            }
          }

          &:hover, &:active {
            background: rgba(244, 67, 54, 0.3);
          }
        }

        &.follow-btn {
          font-size: 0.78rem;
          padding: 6px 9px;

          &.active {
            background: #ff9800;
            color: #000;
            font-weight: 700;
          }
        }
      }

      .speed-group {
        display: flex;
        gap: 2px;
        background: rgba(0, 0, 0, 0.35);
        padding: 2px;
        border-radius: 7px;

        .speed-btn {
          background: transparent;
          border: none;
          color: #888;
          font-size: 0.72rem;
          font-weight: 600;
          padding: 4px 6px;
          border-radius: 5px;
          cursor: pointer;
          touch-action: manipulation;

          &.active {
            background: rgba(255, 255, 255, 0.22);
            color: #fff;
          }

          &:hover:not(.active), &:active:not(.active) {
            color: #ddd;
          }
        }
      }

      /* Mobile layout adjustments */
      @media (max-width: 520px) {
        flex-direction: column;
        align-items: stretch;
        gap: 7px;

        .main-action-group {
          display: flex;
          justify-content: space-between;
          gap: 5px;
          width: 100%;

          .ctrl-btn {
            flex: 1;
            padding: 8px 3px;
            font-size: 0.8rem;
            min-height: 38px;
          }

          .play-btn {
            flex: 1.3;
          }
        }

        .speed-group {
          display: flex;
          justify-content: space-between;
          width: 100%;

          .speed-btn {
            flex: 1;
            text-align: center;
            padding: 6px 2px;
            font-size: 0.76rem;
            min-height: 30px;
          }
        }
      }
    }
  }
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(1.2); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.sync-icon.spinning {
  display: inline-block;
  animation: spin 0.7s linear infinite;
}
</style>

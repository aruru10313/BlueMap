<template>
  <div v-if="blockManager && blockState" class="timelapse-container" :class="{'open': blockState.timelapseOpen}">
    
    <!-- Floating Toast Notification for Date / System Feedback -->
    <transition name="toast-anim">
      <div v-if="blockState.toastMessage" class="timelapse-toast">
        <span class="toast-icon">ℹ️</span>
        <span class="toast-text">{{ blockState.toastMessage }}</span>
      </div>
    </transition>

    <!-- Floating Collapsed Pill (when closed) -->
    <div v-if="!blockState.timelapseOpen" class="collapsed-container">
      <button class="timelapse-toggle-btn" @click="toggleOpen" title="타임랩스 / 실시간 타임라인 열기">
        <span class="icon">⏱</span>
        <span class="label">타임랩스</span>
        <span class="live-dot-mini" :class="{'pulsing': blockState.isPlaying || (isViewingToday && blockState.progress >= 0.999)}"></span>
      </button>

      <button class="quick-sync-btn" :class="{'syncing': blockState.isSyncing}" @click.stop="syncMap" title="맵 및 블록 즉시 동기화">
        <span class="sync-icon" :class="{'spinning': blockState.isSyncing}">🔄</span>
      </button>
    </div>

    <!-- Main Expanded Control Panel -->
    <div v-else class="timelapse-panel">
      <!-- Row 1: Header + Calendar Chip + User Filter Chip + Actions -->
      <div class="panel-header">
        <div class="header-left">
          <div class="title-group">
            <span class="icon">⏱</span>
            <span class="title">타임랩스</span>
          </div>

          <!-- Calendar Selector Chip -->
          <div class="calendar-chip" :class="{'open': calendarOpen}" @click.stop="toggleCalendar" title="날짜별 타임랩스 캘린더">
            <span class="chip-icon">📅</span>
            <span class="chip-label">{{ formattedSelectedDate }}</span>
            <span class="chip-arrow" :class="{'rotated': calendarOpen}">▾</span>

            <!-- Custom Glassmorphism Calendar Dropdown -->
            <div v-if="calendarOpen" class="calendar-dropdown-menu" @click.stop>
              <!-- Calendar Header -->
              <div class="cal-header">
                <button class="cal-nav-btn" @click="prevMonth" title="이전 달">‹</button>
                <span class="cal-title">{{ calendarTitle }}</span>
                <button class="cal-nav-btn" @click="nextMonth" title="다음 달">›</button>
              </div>

              <!-- Weekday Headers -->
              <div class="cal-weekdays">
                <span class="cal-wd sun">일</span>
                <span class="cal-wd">월</span>
                <span class="cal-wd">화</span>
                <span class="cal-wd">수</span>
                <span class="cal-wd">목</span>
                <span class="cal-wd">금</span>
                <span class="cal-wd sat">토</span>
              </div>

              <!-- Days Grid -->
              <div class="cal-days-grid">
                <div
                    v-for="d in calendarDays"
                    :key="d.key"
                    class="cal-day-cell"
                    :class="{
                      'blank': !d.day,
                      'selected': d.isSelected,
                      'today': d.isToday,
                      'has-data': d.hasData
                    }"
                    @click="d.day ? selectCalendarDate(d.day) : null"
                >
                  <span v-if="d.day" class="day-num">{{ d.day }}</span>
                  <span v-if="d.hasData" class="data-dot" title="건축 기록 있음"></span>
                </div>
              </div>

              <!-- Quick Action Shortcuts -->
              <div class="cal-quick-actions">
                <button class="cal-quick-btn" @click="quickSelectToday">☀️ 오늘</button>
                <button class="cal-quick-btn" @click="quickSelectYesterday">🌙 어제</button>
                <button
                    v-if="blockState.availableDates && blockState.availableDates.length > 0"
                    class="cal-quick-btn record-btn"
                    @click="quickSelectLatestRecord"
                >
                  ⚡ 최신 기록일
                </button>
              </div>
            </div>
          </div>

          <!-- User selector chip -->
          <div class="user-chip" :class="{'active': blockState.selectedPlayer, 'open': userDropdownOpen}" @click.stop="toggleUserDropdown" title="유저별 타임랩스 필터">
            <span class="chip-icon">👤</span>
            <span class="chip-label">{{ blockState.selectedPlayer || '전체 유저' }}</span>
            <span class="chip-arrow" :class="{'rotated': userDropdownOpen}">▾</span>
            <button v-if="blockState.selectedPlayer" class="chip-clear" @click.stop="clearPlayerFilter" title="전체 유저로">✕</button>

            <!-- Custom Dropdown Menu -->
            <div v-if="userDropdownOpen" class="user-dropdown-menu" @click.stop>
              <div
                  class="dropdown-item"
                  :class="{'selected': !blockState.selectedPlayer}"
                  @click="selectPlayer('')"
              >
                <span class="item-name">👤 전체 유저</span>
                <span v-if="!blockState.selectedPlayer" class="check">✓</span>
              </div>
              <div
                  v-for="player in blockState.playersList"
                  :key="player"
                  class="dropdown-item"
                  :class="{'selected': blockState.selectedPlayer === player}"
                  @click="selectPlayer(player)"
              >
                <span class="item-name">{{ player }}</span>
                <span v-if="blockState.selectedPlayer === player" class="check">✓</span>
              </div>
            </div>
          </div>
        </div>

        <div class="header-right">
          <button class="sync-btn" :class="{'syncing': blockState.isSyncing}" @click="syncMap" title="맵 및 블록 즉시 동기화">
            <span class="sync-icon" :class="{'spinning': blockState.isSyncing}">🔄</span>
          </button>

          <button class="live-pill-btn" :class="{'active': isViewingToday && blockState.progress >= 0.999 && !blockState.isPlaying}" @click="jumpLive" title="실시간 시점으로 이동">
            <span class="live-dot" :class="{'pulsing': isViewingToday && (blockState.isPlaying || blockState.progress >= 0.999)}"></span>
            <span class="live-text">{{ blockState.isPlaying ? '재생 중' : (isViewingToday && blockState.progress >= 0.999 ? '실시간' : '일시정지') }}</span>
          </button>

          <button class="close-btn" @click="toggleOpen" title="최소화">✕</button>
        </div>
      </div>

      <!-- Row 2: Scrubber Timeline Slider -->
      <div class="slider-row">
        <span class="time-label start" title="시작 시점">{{ startTimeLabel }}</span>
        <div class="slider-wrapper">
          <input
              type="range"
              min="0"
              max="1"
              step="0.0005"
              :value="blockState.progress"
              @input="onSeek"
              class="timeline-slider"
              title="시간순 스크롤 / 탐색"
          />
          <div class="slider-fill" :style="{width: (blockState.progress * 100) + '%'}"></div>
        </div>
        <span class="time-label current" title="현재 시점">
          {{ currentTimeLabel }}
        </span>
      </div>

      <!-- Row 3: Playback Controls & Speed Segmented Control -->
      <div class="controls-row">
        <div class="main-action-group">
          <button class="ctrl-btn jump-btn" @click="jumpStart" title="해당 일자의 처음(00:00)으로 가기">⏮ 처음으로</button>
          <button class="ctrl-btn play-btn" :class="{'playing': blockState.isPlaying}" @click="togglePlay" :title="blockState.isPlaying ? '일시정지' : '재생'">
            <span class="play-icon">{{ blockState.isPlaying ? '⏸' : '▶' }}</span>
            <span class="play-label">{{ blockState.isPlaying ? '일시정지' : '재생' }}</span>
          </button>
        </div>

        <!-- Speed Segmented Control -->
        <div class="speed-group" title="재생 배속">
          <button
              v-for="s in [1, 2, 5, 10, 25, 50]"
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
  data() {
    let now = new Date();
    return {
      userDropdownOpen: false,
      calendarOpen: false,
      viewYear: now.getFullYear(),
      viewMonth: now.getMonth() // 0-indexed
    };
  },
  mounted() {
    document.addEventListener("click", this.handleDocumentClick);
  },
  beforeUnmount() {
    document.removeEventListener("click", this.handleDocumentClick);
  },
  computed: {
    appState() {
      return this.$bluemap ? this.$bluemap.appState : null;
    },
    blockManager() {
      return this.appState ? this.appState.blockManager : (this.$bluemap ? this.$bluemap.blockManager : null);
    },
    blockState() {
      return this.appState && this.appState.blockState ? this.appState.blockState : (this.blockManager ? this.blockManager.data : null);
    },
    isViewingToday() {
      return this.blockManager ? this.blockManager.isViewingToday() : true;
    },
    calendarTitle() {
      return `${this.viewYear}년 ${this.viewMonth + 1}월`;
    },
    calendarDays() {
      let firstDayIndex = new Date(this.viewYear, this.viewMonth, 1).getDay();
      let totalDays = new Date(this.viewYear, this.viewMonth + 1, 0).getDate();

      let blanks = [];
      for (let i = 0; i < firstDayIndex; i++) {
        blanks.push({ day: null, key: `b-${i}` });
      }

      let todayStr = this.blockManager ? this.blockManager.formatDateKey(new Date()) : "";
      let days = [];
      for (let d = 1; d <= totalDays; d++) {
        let m = String(this.viewMonth + 1).padStart(2, "0");
        let dayStr = String(d).padStart(2, "0");
        let fullDateStr = `${this.viewYear}-${m}-${dayStr}`;
        let hasData = this.blockState?.availableDates?.includes(fullDateStr);
        let isSelected = this.blockState?.selectedDate === fullDateStr;
        let isToday = todayStr === fullDateStr;

        days.push({
          day: d,
          dateStr: fullDateStr,
          hasData,
          isSelected,
          isToday,
          key: fullDateStr
        });
      }

      return [...blanks, ...days];
    },
    formattedSelectedDate() {
      if (!this.blockState || !this.blockState.selectedDate) return "날짜 선택";
      let dateStr = this.blockState.selectedDate;
      let todayStr = this.blockManager ? this.blockManager.formatDateKey(new Date()) : "";
      let yestStr = this.blockManager ? this.blockManager.formatDateKey(new Date(Date.now() - 86400000)) : "";

      let parts = dateStr.split("-").map(Number);
      let suffix = "";
      if (dateStr === todayStr) suffix = " (오늘)";
      else if (dateStr === yestStr) suffix = " (어제)";

      return `${parts[0]}. ${String(parts[1]).padStart(2, '0')}. ${String(parts[2]).padStart(2, '0')}${suffix}`;
    },
    startTimeLabel() {
      return "00:00:00";
    },
    currentTimeLabel() {
      if (!this.blockState) return "00:00:00";
      return this.formatClock(this.blockState.currentTime);
    }
  },
  methods: {
    handleDocumentClick(e) {
      if (this.userDropdownOpen && !this.$el?.querySelector(".user-chip")?.contains(e.target)) {
        this.userDropdownOpen = false;
      }
      if (this.calendarOpen && !this.$el?.querySelector(".calendar-chip")?.contains(e.target)) {
        this.calendarOpen = false;
      }
    },
    toggleUserDropdown() {
      this.userDropdownOpen = !this.userDropdownOpen;
      if (this.userDropdownOpen) this.calendarOpen = false;
    },
    toggleCalendar() {
      this.calendarOpen = !this.calendarOpen;
      if (this.calendarOpen) {
        this.userDropdownOpen = false;
        if (this.blockState && this.blockState.selectedDate) {
          let parts = this.blockState.selectedDate.split("-").map(Number);
          this.viewYear = parts[0];
          this.viewMonth = parts[1] - 1;
        }
      }
    },
    prevMonth() {
      if (this.viewMonth === 0) {
        this.viewMonth = 11;
        this.viewYear--;
      } else {
        this.viewMonth--;
      }
    },
    nextMonth() {
      if (this.viewMonth === 11) {
        this.viewMonth = 0;
        this.viewYear++;
      } else {
        this.viewMonth++;
      }
    },
    selectCalendarDate(day) {
      let m = String(this.viewMonth + 1).padStart(2, "0");
      let d = String(day).padStart(2, "0");
      let dateStr = `${this.viewYear}-${m}-${d}`;
      if (this.blockManager) {
        this.blockManager.selectDate(dateStr);
      }
      this.calendarOpen = false;
    },
    quickSelectToday() {
      if (this.blockManager) {
        let todayStr = this.blockManager.formatDateKey(new Date());
        this.blockManager.selectDate(todayStr);
        let now = new Date();
        this.viewYear = now.getFullYear();
        this.viewMonth = now.getMonth();
      }
      this.calendarOpen = false;
    },
    quickSelectYesterday() {
      if (this.blockManager) {
        let yest = new Date(Date.now() - 86400000);
        let yestStr = this.blockManager.formatDateKey(yest);
        this.blockManager.selectDate(yestStr);
        this.viewYear = yest.getFullYear();
        this.viewMonth = yest.getMonth();
      }
      this.calendarOpen = false;
    },
    quickSelectLatestRecord() {
      if (this.blockState && this.blockState.availableDates && this.blockState.availableDates.length > 0) {
        let latest = this.blockState.availableDates[0];
        if (this.blockManager) {
          this.blockManager.selectDate(latest);
          let parts = latest.split("-").map(Number);
          this.viewYear = parts[0];
          this.viewMonth = parts[1] - 1;
        }
      }
      this.calendarOpen = false;
    },
    selectPlayer(player) {
      if (this.blockManager) {
        this.blockManager.setPlayerFilter(player);
      }
      this.userDropdownOpen = false;
    },
    clearPlayerFilter() {
      if (this.blockManager) {
        this.blockManager.setPlayerFilter("");
      }
    },
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
        this.blockManager.pause();
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
        this.blockManager.pause();
        this.blockManager.seek(0.0);
      }
    },
    jumpLive() {
      if (this.blockManager) {
        if (!this.blockManager.isViewingToday()) {
          this.quickSelectToday();
        } else {
          this.blockManager.pause();
          this.blockManager.seek(1.0);
        }
      }
    },
    formatClock(ts) {
      if (!ts) return "00:00:00";
      let d = new Date(ts);
      let h = String(d.getHours()).padStart(2, "0");
      let m = String(d.getMinutes()).padStart(2, "0");
      let s = String(d.getSeconds()).padStart(2, "0");
      return `${h}:${m}:${s}`;
    }
  }
};
</script>

<style lang="scss" scoped>
.timelapse-container {
  position: absolute;
  bottom: calc(18px + env(safe-area-inset-bottom, 0px));
  left: 50%;
  transform: translateX(-50%);
  z-index: 10001;
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  user-select: none;
  font-family: inherit;
  width: calc(100% - 32px);
  max-width: 580px;
  touch-action: manipulation;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  /* Toast Notification */
  .timelapse-toast {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(15, 23, 42, 0.95);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(0, 229, 255, 0.4);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6), 0 0 14px rgba(0, 229, 255, 0.25);
    border-radius: 20px;
    padding: 8px 16px;
    margin-bottom: 10px;
    font-size: 0.82rem;
    font-weight: 600;
    color: #e2e8f0;

    .toast-icon {
      font-size: 0.9rem;
    }

    .toast-text {
      color: #38bdf8;
    }
  }

  .toast-anim-enter-active, .toast-anim-leave-active {
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .toast-anim-enter-from, .toast-anim-leave-to {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
  }

  /* Collapsed Pill */
  .collapsed-container {
    display: flex;
    align-items: center;
    gap: 12px;

    .timelapse-toggle-btn {
      display: flex;
      align-items: center;
      gap: 10px;
      background: rgba(15, 23, 42, 0.92);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      color: #f8fafc;
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 30px;
      padding: 10px 22px;
      font-size: 0.92rem;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
      touch-action: manipulation;
      transition: all 0.2s ease;

      &:hover, &:active {
        background: rgba(30, 41, 59, 0.98);
        border-color: #38bdf8;
        transform: translateY(-2px);
        box-shadow: 0 10px 28px rgba(56, 189, 248, 0.25);
      }

      .icon {
        font-size: 1.15rem;
      }

      .live-dot-mini {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #22c55e;

        &.pulsing {
          background: #38bdf8;
          box-shadow: 0 0 10px #38bdf8;
          animation: pulse-glow 1.5s infinite;
        }
      }
    }

    .quick-sync-btn {
      background: rgba(15, 23, 42, 0.92);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      color: #38bdf8;
      border: 1px solid rgba(56, 189, 248, 0.35);
      border-radius: 50%;
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.2rem;
      cursor: pointer;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
      touch-action: manipulation;
      transition: all 0.2s ease;

      &:hover, &:active {
        background: rgba(56, 189, 248, 0.2);
        border-color: #38bdf8;
        transform: scale(1.06);
      }

      &.syncing {
        border-color: #f59e0b;
        color: #f59e0b;
      }
    }
  }

  /* Main Expanded Panel */
  .timelapse-panel {
    width: 100%;
    box-sizing: border-box;
    background: rgba(15, 23, 42, 0.95);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.16);
    border-radius: 22px;
    padding: 14px 18px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.7);
    display: flex;
    flex-direction: column;
    gap: 12px;

    /* Row 1: Header */
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;

      .header-left {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;

        .title-group {
          display: flex;
          align-items: center;
          gap: 6px;

          .icon {
            font-size: 1.15rem;
          }

          .title {
            font-weight: 700;
            font-size: 0.95rem;
            color: #f8fafc;
            letter-spacing: -0.2px;
          }
        }

        /* Calendar Chip */
        .calendar-chip {
          position: relative;
          display: flex;
          align-items: center;
          gap: 6px;
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(255, 255, 255, 0.18);
          border-radius: 16px;
          padding: 5px 12px;
          cursor: pointer;
          transition: all 0.2s ease;
          user-select: none;

          &:hover, &.open {
            background: rgba(255, 255, 255, 0.14);
            border-color: rgba(56, 189, 248, 0.6);
          }

          .chip-icon {
            font-size: 0.85rem;
          }

          .chip-label {
            color: #38bdf8;
            font-size: 0.82rem;
            font-weight: 600;
            white-space: nowrap;
          }

          .chip-arrow {
            font-size: 0.7rem;
            color: rgba(255, 255, 255, 0.6);
            transition: transform 0.2s ease;
            &.rotated {
              transform: rotate(180deg);
            }
          }

          /* Glassmorphism Calendar Dropdown Menu */
          .calendar-dropdown-menu {
            position: absolute;
            bottom: calc(100% + 10px);
            left: 0;
            width: 270px;
            background: rgba(15, 23, 42, 0.98);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-radius: 18px;
            padding: 12px;
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.8);
            z-index: 10020;
            display: flex;
            flex-direction: column;
            gap: 8px;

            .cal-header {
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 0 4px;

              .cal-title {
                font-weight: 700;
                font-size: 0.88rem;
                color: #f8fafc;
              }

              .cal-nav-btn {
                background: rgba(255, 255, 255, 0.08);
                border: 1px solid rgba(255, 255, 255, 0.14);
                color: #e2e8f0;
                width: 26px;
                height: 26px;
                border-radius: 8px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 1rem;
                cursor: pointer;
                transition: all 0.15s ease;

                &:hover {
                  background: rgba(255, 255, 255, 0.2);
                  color: #fff;
                }
              }
            }

            .cal-weekdays {
              display: grid;
              grid-template-columns: repeat(7, 1fr);
              text-align: center;
              font-size: 0.72rem;
              font-weight: 600;
              color: #64748b;
              padding: 4px 0 2px;

              .sun { color: #f87171; }
              .sat { color: #60a5fa; }
            }

            .cal-days-grid {
              display: grid;
              grid-template-columns: repeat(7, 1fr);
              gap: 3px;

              .cal-day-cell {
                position: relative;
                height: 32px;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                border-radius: 8px;
                font-size: 0.78rem;
                font-weight: 500;
                color: #cbd5e1;
                cursor: pointer;
                transition: all 0.15s ease;

                &.blank {
                  cursor: default;
                }

                &:not(.blank):hover {
                  background: rgba(255, 255, 255, 0.12);
                  color: #fff;
                }

                &.today {
                  border: 1px solid rgba(56, 189, 248, 0.5);
                  font-weight: 700;
                }

                &.selected {
                  background: linear-gradient(135deg, #0284c7, #2563eb) !important;
                  color: #fff !important;
                  font-weight: 700;
                  box-shadow: 0 2px 10px rgba(37, 99, 235, 0.4);
                }

                .data-dot {
                  width: 4px;
                  height: 4px;
                  border-radius: 50%;
                  background: #38bdf8;
                  margin-top: 1px;
                }

                &.selected .data-dot {
                  background: #fff;
                }
              }
            }

            .cal-quick-actions {
              display: flex;
              gap: 6px;
              padding-top: 6px;
              border-top: 1px solid rgba(255, 255, 255, 0.1);

              .cal-quick-btn {
                flex: 1;
                background: rgba(255, 255, 255, 0.08);
                border: 1px solid rgba(255, 255, 255, 0.14);
                color: #cbd5e1;
                border-radius: 8px;
                padding: 5px 0;
                font-size: 0.74rem;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.15s ease;

                &:hover {
                  background: rgba(255, 255, 255, 0.16);
                  color: #fff;
                }

                &.record-btn {
                  color: #38bdf8;
                  border-color: rgba(56, 189, 248, 0.3);
                }
              }
            }
          }
        }

        /* User Chip */
        .user-chip {
          position: relative;
          display: flex;
          align-items: center;
          gap: 6px;
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(255, 255, 255, 0.18);
          border-radius: 16px;
          padding: 5px 12px;
          cursor: pointer;
          transition: all 0.2s ease;
          user-select: none;

          &:hover, &.open {
            background: rgba(255, 255, 255, 0.14);
            border-color: rgba(245, 158, 11, 0.6);
          }

          &.active {
            border-color: #f59e0b;
            background: rgba(245, 158, 11, 0.14);
          }

          .chip-icon {
            font-size: 0.82rem;
          }

          .chip-label {
            color: #f59e0b;
            font-size: 0.82rem;
            font-weight: 600;
            max-width: 100px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .chip-arrow {
            font-size: 0.7rem;
            color: rgba(255, 255, 255, 0.6);
            transition: transform 0.2s ease;
            &.rotated {
              transform: rotate(180deg);
            }
          }

          .chip-clear {
            background: rgba(255, 255, 255, 0.15);
            border: none;
            color: #f87171;
            border-radius: 50%;
            width: 15px;
            height: 15px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.65rem;
            cursor: pointer;
            padding: 0;
            margin-left: 2px;

            &:hover {
              background: rgba(248, 113, 113, 0.35);
              color: #ff4d4f;
            }
          }

          .user-dropdown-menu {
            position: absolute;
            bottom: calc(100% + 10px);
            left: 0;
            min-width: 140px;
            max-width: 200px;
            max-height: 190px;
            overflow-y: auto;
            background: rgba(15, 23, 42, 0.98);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-radius: 16px;
            padding: 5px;
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.8);
            z-index: 10020;
            display: flex;
            flex-direction: column;
            gap: 2px;

            .dropdown-item {
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 7px 12px;
              border-radius: 10px;
              font-size: 0.78rem;
              font-weight: 500;
              color: #e2e8f0;
              cursor: pointer;
              transition: background 0.15s ease;

              &:hover {
                background: rgba(255, 255, 255, 0.12);
                color: #fff;
              }

              &.selected {
                color: #f59e0b;
                font-weight: 700;
                background: rgba(245, 158, 11, 0.18);
              }

              .item-name {
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }

              .check {
                font-size: 0.72rem;
                margin-left: 6px;
                color: #f59e0b;
              }
            }
          }
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 10px;

        .sync-btn {
          background: rgba(56, 189, 248, 0.12);
          border: 1px solid rgba(56, 189, 248, 0.35);
          color: #38bdf8;
          width: 32px;
          height: 32px;
          border-radius: 50%;
          font-size: 0.9rem;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.2s ease;

          &:hover {
            background: rgba(56, 189, 248, 0.25);
            transform: scale(1.06);
          }

          &.syncing {
            border-color: #f59e0b;
            color: #f59e0b;
          }

          .sync-icon.spinning {
            animation: spin 1s linear infinite;
          }
        }

        .live-pill-btn {
          display: flex;
          align-items: center;
          gap: 6px;
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(255, 255, 255, 0.18);
          border-radius: 16px;
          padding: 5px 12px;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.2s ease;

          &:hover {
            background: rgba(255, 255, 255, 0.15);
          }

          &.active {
            background: rgba(34, 197, 94, 0.16);
            border-color: #22c55e;
          }

          .live-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #64748b;
            transition: all 0.2s ease;

            &.pulsing {
              background: #22c55e;
              box-shadow: 0 0 8px #22c55e;
              animation: pulse-glow 1.5s infinite;
            }
          }

          .live-text {
            font-size: 0.78rem;
            font-weight: 700;
            color: #e2e8f0;
          }
        }

        .close-btn {
          background: transparent;
          border: none;
          color: #94a3b8;
          font-size: 0.95rem;
          cursor: pointer;
          width: 28px;
          height: 28px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.15s ease;

          &:hover {
            background: rgba(255, 255, 255, 0.15);
            color: #fff;
          }
        }
      }
    }

    /* Row 2: Slider */
    .slider-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 2px 4px;

      .time-label {
        font-size: 0.8rem;
        font-variant-numeric: tabular-nums;
        font-weight: 600;
        color: #64748b;
        min-width: 54px;

        &.current {
          color: #38bdf8;
          text-align: right;
          font-weight: 700;
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
          background: linear-gradient(90deg, #0284c7, #38bdf8);
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
          background: rgba(255, 255, 255, 0.14);
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
            border: 2px solid #38bdf8;
            box-shadow: 0 0 12px rgba(56, 189, 248, 0.8);
            cursor: pointer;
            transition: transform 0.1s ease;

            &:active {
              transform: scale(1.25);
            }
          }
        }
      }
    }

    /* Row 3: Controls */
    .controls-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding-top: 2px;

      .main-action-group {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .ctrl-btn {
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.16);
        color: #f1f5f9;
        border-radius: 12px;
        padding: 7px 14px;
        font-size: 0.82rem;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        touch-action: manipulation;
        transition: all 0.15s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.16);
          border-color: rgba(255, 255, 255, 0.3);
        }

        &.jump-btn {
          color: #94a3b8;
          &:hover {
            color: #fff;
          }
        }

        &.play-btn {
          background: linear-gradient(135deg, #0284c7, #2563eb);
          border: 1px solid rgba(56, 189, 248, 0.5);
          color: #fff;
          font-weight: 700;
          padding: 7px 16px;
          box-shadow: 0 3px 12px rgba(37, 99, 235, 0.4);

          &:hover {
            background: linear-gradient(135deg, #0369a1, #1d4ed8);
            transform: translateY(-1px);
          }

          &.playing {
            background: linear-gradient(135deg, #e11d48, #be123c);
            border-color: rgba(244, 63, 94, 0.5);
            box-shadow: 0 3px 12px rgba(225, 29, 72, 0.4);
          }
        }
      }

      .speed-group {
        display: flex;
        gap: 3px;
        background: rgba(0, 0, 0, 0.35);
        padding: 3px;
        border-radius: 10px;

        .speed-btn {
          background: transparent;
          border: none;
          color: #94a3b8;
          font-size: 0.74rem;
          font-weight: 600;
          padding: 4px 7px;
          border-radius: 7px;
          cursor: pointer;
          touch-action: manipulation;
          transition: all 0.15s ease;

          &:hover {
            color: #fff;
            background: rgba(255, 255, 255, 0.1);
          }

          &.active {
            background: rgba(255, 255, 255, 0.2);
            color: #38bdf8;
            font-weight: 700;
          }
        }
      }
    }
  }
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse-glow {
  0% {
    transform: scale(0.95);
    opacity: 0.85;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
  100% {
    transform: scale(0.95);
    opacity: 0.85;
  }
}
</style>
